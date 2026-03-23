-- ============================================================
-- Huawei Cloud RDS for PostgreSQL - Database DDL
-- Updated: 2026-01-28
-- Target: PostgreSQL (Huawei Cloud RDS for PostgreSQL)
--
-- Note:
--  - Brief purpose comments added for each function, table, and trigger.
-- ============================================================


-- -----------------------------------------------------------------------------
-- SCHEMAS: Logical separation of concerns (utilities, lookups, LMS core, AI, etc.)
-- -----------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS util;     -- Utility helpers (uuid, triggers, shared functions)
CREATE SCHEMA IF NOT EXISTS lookups;  -- Reference/enum-like tables (roles, statuses, types)
CREATE SCHEMA IF NOT EXISTS lms;      -- Core LMS domain (schools, users, classes, assessments, etc.)
CREATE SCHEMA IF NOT EXISTS kb;       -- Knowledge base / RAG storage (docs, chunks, embeddings)
CREATE SCHEMA IF NOT EXISTS ai;       -- AI model registry + inference traces
CREATE SCHEMA IF NOT EXISTS edge;     -- Edge devices + sync/outbox/inbox
CREATE SCHEMA IF NOT EXISTS audit;    -- Append-only audit/event logging

-- -----------------------------------------------------------------------------
-- FUNCTION: util.gen_uuid_v4()
-- Purpose: Generates UUID v4-like values without requiring extensions.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION util.gen_uuid_v4()
RETURNS uuid
LANGUAGE plpgsql
VOLATILE
AS $$
DECLARE
  v text;
  variant text;
BEGIN
  v := md5(random()::text || clock_timestamp()::text || pg_backend_pid()::text);

  -- Set version (4) and variant bits to resemble RFC4122 UUIDv4
  v := substr(v, 1, 12) || '4' || substr(v, 14);
  variant := substr('89ab', floor(random() * 4)::int + 1, 1);
  v := substr(v, 1, 16) || variant || substr(v, 18);

  RETURN (substr(v,1,8) || '-' ||
          substr(v,9,4) || '-' ||
          substr(v,13,4) || '-' ||
          substr(v,17,4) || '-' ||
          substr(v,21,12))::uuid;
END;
$$;

-- -----------------------------------------------------------------------------
-- FUNCTION: util.tg_touch_row()
-- Purpose: Standardizes insert/update behavior for sync-aware rows.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION util.tg_touch_row()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    NEW.created_at := COALESCE(NEW.created_at, NOW());
    NEW.updated_at := NOW();
    NEW.sync_version := COALESCE(NULLIF(NEW.sync_version, 0), 1);
  ELSE
    NEW.updated_at := NOW();
    NEW.sync_version := COALESCE(OLD.sync_version, 0) + 1;
  END IF;
  RETURN NEW;
END;
$$;

-- =============================================================================
-- LOOKUPS: Shared controlled vocabularies used across the LMS
-- =============================================================================

-- Table: lookups.roles
-- Purpose: Global roles that can be attached to users (many-to-many via lms.user_roles).
CREATE TABLE IF NOT EXISTS lookups.roles (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  code varchar(50) UNIQUE NOT NULL,   -- student, teacher, admin, parent
  name varchar(100) NOT NULL
);

-- Table: lookups.enrolment_status
-- Purpose: Enrolment lifecycle states for class/subject enrolments.
CREATE TABLE IF NOT EXISTS lookups.enrolment_status (
  code varchar(50) PRIMARY KEY,       -- active, dropped, completed
  name varchar(100) NOT NULL
);

-- Table: lookups.question_type
-- Purpose: Question formats to guide UI rendering and grading pipelines.
CREATE TABLE IF NOT EXISTS lookups.question_type (
  code varchar(50) PRIMARY KEY,       -- short_answer, structured, mcq, true_false, essay
  name varchar(100) NOT NULL
);

-- Table: lookups.exam_style
-- Purpose: Declares whether content is past-paper style or teacher-created.
CREATE TABLE IF NOT EXISTS lookups.exam_style (
  code varchar(50) PRIMARY KEY,       -- past_paper, teacher_created
  name varchar(100) NOT NULL
);

-- Table: lookups.grading_status
-- Purpose: Grading pipeline states for attempts (pending → auto_graded → reviewed).
CREATE TABLE IF NOT EXISTS lookups.grading_status (
  code varchar(50) PRIMARY KEY,       -- pending, auto_graded, reviewed
  name varchar(100) NOT NULL
);

-- Table: lookups.risk_level
-- Purpose: Risk buckets used for mastery snapshots / learner analytics.
CREATE TABLE IF NOT EXISTS lookups.risk_level (
  code varchar(20) PRIMARY KEY,       -- low, medium, high
  name varchar(100) NOT NULL
);

-- Table: lookups.exam_board
-- Purpose: Exam boards supported (e.g., ZIMSEC) to scope curriculum variations.
CREATE TABLE IF NOT EXISTS lookups.exam_board (
  code varchar(50) PRIMARY KEY,       -- zimsec, cambridge
  name varchar(100) NOT NULL
);

-- Table: lookups.contact_channel
-- Purpose: Contact method types (email, mobile, WhatsApp, etc.).
CREATE TABLE IF NOT EXISTS lookups.contact_channel (
  code varchar(50) PRIMARY KEY,       -- mobile, email, whatsapp, telegram, landline
  name varchar(100) NOT NULL
);

-- Table: lookups.address_type
-- Purpose: Address usage types (home, school, postal, etc.).
CREATE TABLE IF NOT EXISTS lookups.address_type (
  code varchar(50) PRIMARY KEY,       -- home, postal, school, work
  name varchar(100) NOT NULL
);

-- Table: lookups.document_type
-- Purpose: Student document categories (IDs, birth certs, reports, etc.).
CREATE TABLE IF NOT EXISTS lookups.document_type (
  code varchar(50) PRIMARY KEY,       -- student_id, birth_cert, report, script, other
  name varchar(100) NOT NULL
);

-- Table: lookups.parent_relationship
-- Purpose: Relationship types between students and parents/guardians.
CREATE TABLE IF NOT EXISTS lookups.parent_relationship (
  code varchar(50) PRIMARY KEY,       -- mother, father, guardian, other
  name varchar(100) NOT NULL
);

-- Table: lookups.assessment_enrollment_status
-- Purpose: Status of assignment participation (assigned/completed/late).
CREATE TABLE IF NOT EXISTS lookups.assessment_enrollment_status (
  code varchar(50) PRIMARY KEY,       -- assigned, completed, late
  name varchar(100) NOT NULL
);

-- =============================================================================
-- LMS CORE: Schools, users, membership, subjects, classes, enrolments
-- =============================================================================

-- Table: lms.schools
-- Purpose: Tenant root entity; isolates data per school.
CREATE TABLE IF NOT EXISTS lms.schools (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  code varchar(64) UNIQUE NOT NULL,
  name varchar(255) NOT NULL,
  country_code varchar(10) NOT NULL DEFAULT 'ZW',

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);

-- Trigger: trg_schools_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_schools_touch
BEFORE UPDATE ON lms.schools
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- Table: lms.users
-- Purpose: Global identity record for all people (students/teachers/admin/parents).
CREATE TABLE IF NOT EXISTS lms.users (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  external_id varchar(100),           -- optional external/SSO identifier

  email varchar(255) UNIQUE NOT NULL,
  phone_number varchar(255) UNIQUE NOT NULL,
  first_name varchar(50) NOT NULL,
  last_name varchar(50) NOT NULL,
  username varchar(100) UNIQUE,
  password_hash varchar(255),

  is_active boolean NOT NULL DEFAULT TRUE,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);

-- Trigger: trg_users_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_users_touch
BEFORE UPDATE ON lms.users
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- Table: lms.user_roles
-- Purpose: Assigns one or more roles to a user (many-to-many).
CREATE TABLE IF NOT EXISTS lms.user_roles (
  user_id uuid NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,
  role_id uuid NOT NULL REFERENCES lookups.roles(id) ON DELETE CASCADE,
  PRIMARY KEY (user_id, role_id)
);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON lms.user_roles(role_id); -- role-based lookup acceleration

-- Table: lms.school_users
-- Purpose: Membership link between users and schools (supports multi-school).
CREATE TABLE IF NOT EXISTS lms.school_users (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  school_id uuid NOT NULL REFERENCES lms.schools(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,

  is_active boolean NOT NULL DEFAULT TRUE,
  joined_at timestamptz NOT NULL DEFAULT NOW(),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  UNIQUE (school_id, user_id),

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_school_users_school ON lms.school_users(school_id);
CREATE INDEX IF NOT EXISTS idx_school_users_user   ON lms.school_users(user_id);

-- Trigger: trg_school_users_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_school_users_touch
BEFORE UPDATE ON lms.school_users
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- Table: lms.subjects
-- Purpose: Global curriculum subjects (Math, English, etc.).
CREATE TABLE IF NOT EXISTS lms.subjects (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  code varchar(50) UNIQUE NOT NULL,
  name varchar(200) NOT NULL,
  exam_board_code varchar(50) REFERENCES lookups.exam_board(code),
  description text,
  subject_attributes jsonb,
  is_active boolean NOT NULL DEFAULT TRUE,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);

-- Trigger: trg_subjects_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_subjects_touch
BEFORE UPDATE ON lms.subjects
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- Table: lms.topics
-- Purpose: Topics within a subject for structuring content (e.g., Algebra → Quadratics).
CREATE TABLE IF NOT EXISTS lms.topics (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  subject_id uuid NOT NULL REFERENCES lms.subjects(id) ON DELETE CASCADE,
  code varchar(100) NOT NULL,
  name varchar(200) NOT NULL,
  description text,
  objectives text,
  sequence_index int,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (subject_id, code)
);
ALTER TABLE lms.topics
  ADD COLUMN IF NOT EXISTS objectives text;
CREATE INDEX IF NOT EXISTS idx_topics_subject_id ON lms.topics(subject_id);

-- Trigger: trg_topics_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_topics_touch
BEFORE UPDATE ON lms.topics
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- Table: lms.skills
-- Purpose: Atomic skills to support mastery modeling (DKT) and learning analytics.
CREATE TABLE IF NOT EXISTS lms.skills (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  subject_id uuid NOT NULL REFERENCES lms.subjects(id) ON DELETE CASCADE,
  topic_id uuid REFERENCES lms.topics(id) ON DELETE SET NULL,
  code varchar(100) NOT NULL,
  name varchar(200) NOT NULL,
  description text,
  difficulty smallint,
  sequence_index int,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (subject_id, code)
);
CREATE INDEX IF NOT EXISTS idx_skills_subject_id ON lms.skills(subject_id);
CREATE INDEX IF NOT EXISTS idx_skills_topic_id   ON lms.skills(topic_id);

-- Trigger: trg_skills_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_skills_touch
BEFORE UPDATE ON lms.skills
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- Table: lms.skill_prerequisites
-- Purpose: Directed edges defining prerequisite relationships between skills.
CREATE TABLE IF NOT EXISTS lms.skill_prerequisites (
  skill_id uuid NOT NULL REFERENCES lms.skills(id) ON DELETE CASCADE,
  prerequisite_skill_id uuid NOT NULL REFERENCES lms.skills(id) ON DELETE CASCADE,
  PRIMARY KEY (skill_id, prerequisite_skill_id)
);

-- Table: lms.classes
-- Purpose: School-scoped classes/streams (e.g., Form 4A) for enrolment and scheduling.
CREATE TABLE IF NOT EXISTS lms.classes (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  school_id uuid NOT NULL REFERENCES lms.schools(id) ON DELETE CASCADE,

  code varchar(100) NOT NULL,
  name varchar(200) NOT NULL,
  grade_level varchar(50),
  academic_year varchar(16),
  homeroom_teacher_id uuid REFERENCES lms.users(id),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (school_id, code)
);
CREATE INDEX IF NOT EXISTS idx_classes_school_id  ON lms.classes(school_id);
CREATE INDEX IF NOT EXISTS idx_classes_teacher_id ON lms.classes(homeroom_teacher_id);

-- Trigger: trg_classes_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_classes_touch
BEFORE UPDATE ON lms.classes
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- Table: lms.enrolments
-- Purpose: Student membership within a class (class roster).
CREATE TABLE IF NOT EXISTS lms.enrolments (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  class_id uuid NOT NULL REFERENCES lms.classes(id) ON DELETE CASCADE,
  student_id uuid NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,
  enrolment_status_code varchar(50) NOT NULL REFERENCES lookups.enrolment_status(code),
  enrolled_at timestamptz NOT NULL DEFAULT NOW(),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (class_id, student_id)
);
CREATE INDEX IF NOT EXISTS idx_enrolments_class_id   ON lms.enrolments(class_id);
CREATE INDEX IF NOT EXISTS idx_enrolments_student_id ON lms.enrolments(student_id);

-- Trigger: trg_enrolments_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_enrolments_touch
BEFORE UPDATE ON lms.enrolments
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- Table: lms.class_subjects
-- Purpose: Canonical mapping of (class ↔ subject ↔ teacher) per academic year/term.
CREATE TABLE IF NOT EXISTS lms.class_subjects (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  school_id uuid NOT NULL REFERENCES lms.schools(id) ON DELETE CASCADE,
  class_id uuid NOT NULL REFERENCES lms.classes(id) ON DELETE CASCADE,
  subject_id uuid NOT NULL REFERENCES lms.subjects(id) ON DELETE CASCADE,

  teacher_id uuid REFERENCES lms.users(id) ON DELETE SET NULL,

  academic_year varchar(16) NOT NULL,
  term varchar(16),

  name varchar(200),
  is_active boolean NOT NULL DEFAULT TRUE,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (class_id, subject_id, academic_year, term)
);
CREATE INDEX IF NOT EXISTS idx_class_subjects_class   ON lms.class_subjects(class_id);
CREATE INDEX IF NOT EXISTS idx_class_subjects_subject ON lms.class_subjects(subject_id);
CREATE INDEX IF NOT EXISTS idx_class_subjects_teacher ON lms.class_subjects(teacher_id);

-- Trigger: trg_class_subjects_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_class_subjects_touch
BEFORE UPDATE ON lms.class_subjects
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- Table: lms.term_forecasts
-- Purpose: Teacher term expectations for a subject (subset of curriculum topics).
CREATE TABLE IF NOT EXISTS lms.term_forecasts (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  class_subject_id uuid NOT NULL REFERENCES lms.class_subjects(id) ON DELETE CASCADE,
  term varchar(16) NOT NULL,
  academic_year varchar(16) NOT NULL,

  expected_coverage_pct numeric(5,2) NOT NULL DEFAULT 0 CHECK (expected_coverage_pct BETWEEN 0 AND 100),
  expected_topic_ids jsonb,
  notes text,

  created_by uuid REFERENCES lms.users(id) ON DELETE SET NULL,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (class_subject_id, term, academic_year)
);
CREATE INDEX IF NOT EXISTS idx_term_forecasts_class_subject ON lms.term_forecasts(class_subject_id);
CREATE INDEX IF NOT EXISTS idx_term_forecasts_term          ON lms.term_forecasts(term);

-- Trigger: trg_term_forecasts_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_term_forecasts_touch
BEFORE UPDATE ON lms.term_forecasts
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- Table: lms.student_subject_enrolments
-- Purpose: Explicit subject enrolment per student (supports electives/multiple subjects).
CREATE TABLE IF NOT EXISTS lms.student_subject_enrolments (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  class_subject_id uuid NOT NULL REFERENCES lms.class_subjects(id) ON DELETE CASCADE,
  student_id uuid NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,
  status_code varchar(50) NOT NULL REFERENCES lookups.enrolment_status(code),
  enrolled_at timestamptz NOT NULL DEFAULT NOW(),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (class_subject_id, student_id)
);
CREATE INDEX IF NOT EXISTS idx_student_subject_enrolments_student
  ON lms.student_subject_enrolments(student_id);

-- Trigger: trg_student_subject_enrolments_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_student_subject_enrolments_touch
BEFORE UPDATE ON lms.student_subject_enrolments
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- -----------------------------------------------------------------------------
-- RESOURCES: Files/links stored in object storage; DB keeps metadata and pointers
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lms.resources (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  school_id uuid NOT NULL REFERENCES lms.schools(id) ON DELETE CASCADE,
  subject_id uuid REFERENCES lms.subjects(id) ON DELETE SET NULL,

  uploaded_by uuid NOT NULL REFERENCES lms.users(id),
  name varchar(255) NOT NULL,
  original_name varchar(255) NOT NULL,
  mime_type varchar(128) NOT NULL,
  res_type varchar(16) NOT NULL CHECK (res_type IN ('document','image','video','other')),
  size_bytes bigint NOT NULL,
  url varchar(1024) NOT NULL,
  storage_key varchar(512),
  storage_path varchar(1024),
  tags text[],
  content_type varchar(64),
  content_body text,
  publish_at timestamptz,
  downloads int NOT NULL DEFAULT 0,
  display_order int NOT NULL DEFAULT 0,

  status varchar(16) NOT NULL DEFAULT 'active' CHECK (status IN ('active','draft','archived')),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_resources_school    ON lms.resources(school_id);
CREATE INDEX IF NOT EXISTS idx_resources_subject   ON lms.resources(subject_id);
CREATE INDEX IF NOT EXISTS idx_resources_downloads ON lms.resources(downloads DESC);

-- Table: lms.topic_resources
-- Purpose: Many-to-many mapping of resources to curriculum topics.
CREATE TABLE IF NOT EXISTS lms.topic_resources (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  topic_id uuid NOT NULL REFERENCES lms.topics(id) ON DELETE CASCADE,
  resource_id uuid NOT NULL REFERENCES lms.resources(id) ON DELETE CASCADE,
  display_order int NOT NULL DEFAULT 0,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_topic_resources_topic_resource UNIQUE (topic_id, resource_id)
);
CREATE INDEX IF NOT EXISTS idx_topic_resources_topic
  ON lms.topic_resources(topic_id, display_order, created_at);
CREATE INDEX IF NOT EXISTS idx_topic_resources_resource
  ON lms.topic_resources(resource_id);

-- Trigger: trg_topic_resources_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_topic_resources_touch
BEFORE UPDATE ON lms.topic_resources
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- Trigger: trg_resources_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_resources_touch
BEFORE UPDATE ON lms.resources
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- -----------------------------------------------------------------------------
-- QUESTION BANK: Questions + skill mapping + marking schemes (rubric-aware)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lms.questions (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  subject_id uuid NOT NULL REFERENCES lms.subjects(id) ON DELETE CASCADE,
  topic_id uuid REFERENCES lms.topics(id) ON DELETE SET NULL,
  author_id uuid REFERENCES lms.users(id),

  code varchar(100),
  stem text NOT NULL,
  question_type_code varchar(50) NOT NULL REFERENCES lookups.question_type(code),
  max_mark numeric(8,2) NOT NULL,
  difficulty smallint,
  exam_style_code varchar(50) REFERENCES lookups.exam_style(code),
  source_year smallint,

  rubric_json jsonb,

  is_active boolean NOT NULL DEFAULT TRUE,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_questions_subject_id ON lms.questions(subject_id);
CREATE INDEX IF NOT EXISTS idx_questions_topic_id   ON lms.questions(topic_id);

-- Trigger: trg_questions_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_questions_touch
BEFORE UPDATE ON lms.questions
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.question_skills (
  question_id uuid NOT NULL REFERENCES lms.questions(id) ON DELETE CASCADE,
  skill_id uuid NOT NULL REFERENCES lms.skills(id) ON DELETE CASCADE,
  PRIMARY KEY (question_id, skill_id)
);
CREATE INDEX IF NOT EXISTS idx_question_skills_skill_id ON lms.question_skills(skill_id);

CREATE TABLE IF NOT EXISTS lms.marking_schemes (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  question_id uuid NOT NULL REFERENCES lms.questions(id) ON DELETE CASCADE,
  version int NOT NULL DEFAULT 1,
  total_mark numeric(8,2) NOT NULL,
  scheme_source varchar(100),
  is_active boolean NOT NULL DEFAULT TRUE,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (question_id, version)
);

-- Trigger: trg_marking_schemes_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_marking_schemes_touch
BEFORE UPDATE ON lms.marking_schemes
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.marking_scheme_items (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  marking_scheme_id uuid NOT NULL REFERENCES lms.marking_schemes(id) ON DELETE CASCADE,
  step_index int NOT NULL,
  description text NOT NULL,
  mark_value numeric(8,2) NOT NULL,
  rubric_code varchar(100),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (marking_scheme_id, step_index)
);
CREATE INDEX IF NOT EXISTS idx_marking_items_scheme_id ON lms.marking_scheme_items(marking_scheme_id);

-- Trigger: trg_marking_scheme_items_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_marking_scheme_items_touch
BEFORE UPDATE ON lms.marking_scheme_items
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- -----------------------------------------------------------------------------
-- ASSESSMENTS: Unified quiz/test/assignment/project/exam pipeline
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lms.assessments (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  school_id uuid NOT NULL REFERENCES lms.schools(id) ON DELETE CASCADE,
  subject_id uuid NOT NULL REFERENCES lms.subjects(id) ON DELETE CASCADE,

  name varchar(255) NOT NULL,
  description text,

  assessment_type varchar(16) NOT NULL CHECK (assessment_type IN ('quiz','test','assignment','project','exam','practice')),
  visibility varchar(16) NOT NULL DEFAULT 'private' CHECK (visibility IN ('private','shared','school')),

  time_limit_min int,
  attempts_allowed int,

  max_score numeric(10,2) NOT NULL,
  weight_pct numeric(5,2) NOT NULL DEFAULT 0 CHECK (weight_pct BETWEEN 0 AND 100),

  resource_id uuid REFERENCES lms.resources(id) ON DELETE SET NULL,

  is_ai_enhanced boolean NOT NULL DEFAULT FALSE,
  status varchar(16) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft','published','archived')),

  created_by uuid NOT NULL REFERENCES lms.users(id),
  last_modified_by uuid NOT NULL REFERENCES lms.users(id),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_assessments_school_subject ON lms.assessments(school_id, subject_id);
CREATE INDEX IF NOT EXISTS idx_assessments_status         ON lms.assessments(school_id, status, created_at DESC);

-- Trigger: trg_assessments_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_assessments_touch
BEFORE UPDATE ON lms.assessments
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.assessment_questions (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  assessment_id uuid NOT NULL REFERENCES lms.assessments(id) ON DELETE CASCADE,
  question_id uuid NOT NULL REFERENCES lms.questions(id),
  sequence_index int NOT NULL,
  points numeric(10,2) NOT NULL CHECK (points >= 0),

  rubric_scheme_id uuid REFERENCES lms.marking_schemes(id) ON DELETE SET NULL,
  rubric_scheme_version int,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (assessment_id, question_id),
  UNIQUE (assessment_id, sequence_index)
);
CREATE INDEX IF NOT EXISTS idx_assessment_questions_assessment ON lms.assessment_questions(assessment_id);

-- Trigger: trg_assessment_questions_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_assessment_questions_touch
BEFORE UPDATE ON lms.assessment_questions
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.assessment_assignments (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  assessment_id uuid NOT NULL REFERENCES lms.assessments(id) ON DELETE CASCADE,
  class_id uuid REFERENCES lms.classes(id) ON DELETE SET NULL,
  class_subject_id uuid REFERENCES lms.class_subjects(id) ON DELETE SET NULL,

  assigned_by uuid NOT NULL REFERENCES lms.users(id),
  title varchar(200),
  instructions text,
  start_time timestamptz,
  due_time timestamptz,

  is_published boolean NOT NULL DEFAULT FALSE,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_assessment_assignments_class_subject ON lms.assessment_assignments(class_subject_id);
CREATE INDEX IF NOT EXISTS idx_assessment_assignments_assessment   ON lms.assessment_assignments(assessment_id);
CREATE INDEX IF NOT EXISTS idx_assessment_assignments_class        ON lms.assessment_assignments(class_id);
CREATE INDEX IF NOT EXISTS idx_assessment_assignments_assignedby   ON lms.assessment_assignments(assigned_by);

-- Trigger: trg_assessment_assignments_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_assessment_assignments_touch
BEFORE UPDATE ON lms.assessment_assignments
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.assessment_enrollments (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  assessment_assignment_id uuid NOT NULL REFERENCES lms.assessment_assignments(id) ON DELETE CASCADE,
  student_id uuid NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,
  status_code varchar(50) NOT NULL REFERENCES lookups.assessment_enrollment_status(code),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (assessment_assignment_id, student_id)
);
CREATE INDEX IF NOT EXISTS idx_assessment_enrollments_assignment ON lms.assessment_enrollments(assessment_assignment_id);
CREATE INDEX IF NOT EXISTS idx_assessment_enrollments_student    ON lms.assessment_enrollments(student_id);

-- Trigger: trg_assessment_enrollments_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_assessment_enrollments_touch
BEFORE UPDATE ON lms.assessment_enrollments
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.assessment_attempts (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  assessment_enrollment_id uuid NOT NULL REFERENCES lms.assessment_enrollments(id) ON DELETE CASCADE,
  attempt_number int NOT NULL DEFAULT 1,

  started_at timestamptz NOT NULL DEFAULT NOW(),
  submitted_at timestamptz,

  total_score numeric(10,2),
  max_score numeric(10,2),
  final_score numeric(10,2),
  final_grade varchar(8),

  submission_type varchar(16),
  original_filename varchar(255),
  file_type varchar(128),
  file_size_bytes bigint,
  storage_path varchar(1024),

  grading_status_code varchar(50) NOT NULL REFERENCES lookups.grading_status(code),
  ai_confidence numeric(6,4),

  attempt_trace_id varchar(64),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (assessment_enrollment_id, attempt_number)
);
CREATE INDEX IF NOT EXISTS idx_assessment_attempts_enrollment ON lms.assessment_attempts(assessment_enrollment_id);
CREATE INDEX IF NOT EXISTS idx_assessment_attempts_latest     ON lms.assessment_attempts(assessment_enrollment_id, attempt_number DESC);

-- Trigger: trg_assessment_attempts_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_assessment_attempts_touch
BEFORE UPDATE ON lms.assessment_attempts
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.attempt_answers (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  assessment_attempt_id uuid NOT NULL REFERENCES lms.assessment_attempts(id) ON DELETE CASCADE,
  assessment_question_id uuid NOT NULL REFERENCES lms.assessment_questions(id),

  student_answer_text text,
  student_answer_blob jsonb,
  submission_type varchar(16),
  text_content text,
  external_assessment_data jsonb,

  handwriting_resource_id uuid REFERENCES lms.resources(id) ON DELETE SET NULL,
  ocr_text text,
  ocr_confidence numeric(6,4),
  ocr_engine varchar(64),
  ocr_language varchar(32),
  ocr_metadata jsonb,

  ai_score numeric(10,2),
  human_score numeric(10,2),
  max_score numeric(10,2) NOT NULL,

  ai_confidence numeric(6,4),
  requires_review boolean NOT NULL DEFAULT FALSE,
  feedback_text text,
  graded_at timestamptz,

  answer_trace_id varchar(64),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (assessment_attempt_id, assessment_question_id)
);
CREATE INDEX IF NOT EXISTS idx_attempt_answers_attempt ON lms.attempt_answers(assessment_attempt_id);
CREATE INDEX IF NOT EXISTS idx_attempt_answers_trace   ON lms.attempt_answers(answer_trace_id);

-- Trigger: trg_attempt_answers_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_attempt_answers_touch
BEFORE UPDATE ON lms.attempt_answers
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.assessment_results (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),

  assessment_assignment_id uuid NOT NULL REFERENCES lms.assessment_assignments(id) ON DELETE CASCADE,
  student_id uuid NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,

  finalized_attempt_id uuid REFERENCES lms.assessment_attempts(id) ON DELETE SET NULL,

  expected_mark numeric(10,2),
  actual_mark numeric(10,2),
  grade varchar(8),
  feedback text DEFAULT '',

  submitted_at timestamptz,
  graded_at timestamptz,
  status varchar(16) NOT NULL DEFAULT 'draft'
    CHECK (status IN ('draft','published','archived')),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (assessment_assignment_id, student_id)
);
CREATE INDEX IF NOT EXISTS idx_assessment_results_assignment ON lms.assessment_results(assessment_assignment_id);
CREATE INDEX IF NOT EXISTS idx_assessment_results_student    ON lms.assessment_results(student_id, graded_at DESC);

-- Trigger: trg_assessment_results_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_assessment_results_touch
BEFORE UPDATE ON lms.assessment_results
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.answer_attachments (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  attempt_answer_id uuid NOT NULL REFERENCES lms.attempt_answers(id) ON DELETE CASCADE,
  resource_id uuid REFERENCES lms.resources(id) ON DELETE SET NULL,

  file_name varchar(255),
  storage_path varchar(1024),
  mime_type varchar(128),
  size_bytes bigint,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_answer_attachments_answer ON lms.answer_attachments(attempt_answer_id);

-- Trigger: trg_answer_attachments_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_answer_attachments_touch
BEFORE UPDATE ON lms.answer_attachments
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.grading_overrides (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  attempt_answer_id uuid NOT NULL REFERENCES lms.attempt_answers(id) ON DELETE CASCADE,
  teacher_id uuid NOT NULL REFERENCES lms.users(id),

  old_score numeric(10,2),
  new_score numeric(10,2) NOT NULL,
  reason text,
  overridden_at timestamptz NOT NULL DEFAULT NOW(),
  linked_trace_id varchar(64),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_grading_overrides_answer  ON lms.grading_overrides(attempt_answer_id);
CREATE INDEX IF NOT EXISTS idx_grading_overrides_teacher ON lms.grading_overrides(teacher_id, overridden_at DESC);

-- Trigger: trg_grading_overrides_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_grading_overrides_touch
BEFORE UPDATE ON lms.grading_overrides
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- -----------------------------------------------------------------------------
-- DKT EVENTS: Model-friendly interaction history
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lms.interaction_events (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  school_id uuid NOT NULL REFERENCES lms.schools(id) ON DELETE CASCADE,
  edge_node_id uuid,

  student_id uuid NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,
  subject_id uuid REFERENCES lms.subjects(id) ON DELETE SET NULL,
  skill_id uuid NOT NULL REFERENCES lms.skills(id) ON DELETE CASCADE,

  assessment_attempt_id uuid REFERENCES lms.assessment_attempts(id) ON DELETE SET NULL,
  attempt_answer_id uuid REFERENCES lms.attempt_answers(id) ON DELETE SET NULL,

  is_correct smallint NOT NULL CHECK (is_correct IN (0,1)),
  score numeric(10,2),
  max_score numeric(10,2),
  event_time timestamptz NOT NULL DEFAULT NOW(),

  trace_id varchar(64),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_interactions_student_time ON lms.interaction_events(student_id, event_time);
CREATE INDEX IF NOT EXISTS idx_interactions_skill_time   ON lms.interaction_events(skill_id, event_time);

-- Trigger: trg_interaction_events_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_interaction_events_touch
BEFORE UPDATE ON lms.interaction_events
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- -----------------------------------------------------------------------------
-- MASTERY SNAPSHOTS: “read-optimized” mastery rollups for dashboards
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lms.mastery_snapshots (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  student_id uuid NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,
  subject_id uuid NOT NULL REFERENCES lms.subjects(id),
  snapshot_time timestamptz NOT NULL DEFAULT NOW(),
  source varchar(50) NOT NULL CHECK (source IN ('dkt_update','batch_recalc')),
  average_mastery numeric(6,4),
  risk_level_code varchar(20) REFERENCES lookups.risk_level(code),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (student_id, subject_id, snapshot_time)
);
CREATE INDEX IF NOT EXISTS idx_mastery_latest ON lms.mastery_snapshots(student_id, subject_id, snapshot_time DESC);

-- Trigger: trg_mastery_snapshots_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_mastery_snapshots_touch
BEFORE UPDATE ON lms.mastery_snapshots
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.mastery_snapshot_skills (
  mastery_snapshot_id uuid NOT NULL REFERENCES lms.mastery_snapshots(id) ON DELETE CASCADE,
  skill_id uuid NOT NULL REFERENCES lms.skills(id),
  mastery_prob numeric(6,4) NOT NULL,
  PRIMARY KEY (mastery_snapshot_id, skill_id)
);
CREATE INDEX IF NOT EXISTS idx_mastery_snapshot_skills_skill ON lms.mastery_snapshot_skills(skill_id);

-- -----------------------------------------------------------------------------
-- KB / RAG: Document store + chunking + embeddings (portable arrays)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kb.kb_versions (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  school_id uuid NOT NULL REFERENCES lms.schools(id) ON DELETE CASCADE,
  subject_id uuid REFERENCES lms.subjects(id) ON DELETE SET NULL,
  name varchar(255) NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'active' CHECK (status IN ('active','archived')),
  created_by uuid NOT NULL REFERENCES lms.users(id),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_kb_versions_school_subject ON kb.kb_versions(school_id, subject_id);

-- Trigger: trg_kb_versions_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_kb_versions_touch
BEFORE UPDATE ON kb.kb_versions
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS kb.kb_documents (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  kb_version_id uuid NOT NULL REFERENCES kb.kb_versions(id) ON DELETE CASCADE,
  resource_id uuid NOT NULL REFERENCES lms.resources(id) ON DELETE CASCADE,

  status varchar(16) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft','approved','archived')),
  approved_by uuid REFERENCES lms.users(id),
  approved_at timestamptz,

  metadata jsonb,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_kb_documents_version ON kb.kb_documents(kb_version_id, status);

-- Trigger: trg_kb_documents_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_kb_documents_touch
BEFORE UPDATE ON kb.kb_documents
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS kb.kb_chunks (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  document_id uuid NOT NULL REFERENCES kb.kb_documents(id) ON DELETE CASCADE,
  chunk_index int NOT NULL,
  text text NOT NULL,
  token_count int,
  metadata jsonb,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (document_id, chunk_index)
);
CREATE INDEX IF NOT EXISTS idx_kb_chunks_document ON kb.kb_chunks(document_id);

-- Trigger: trg_kb_chunks_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_kb_chunks_touch
BEFORE UPDATE ON kb.kb_chunks
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS kb.kb_embeddings (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  chunk_id uuid NOT NULL REFERENCES kb.kb_chunks(id) ON DELETE CASCADE,

  embedding_dim int NOT NULL,
  embedding float4[],
  vector_store varchar(64),
  vector_id varchar(255),
  index_name varchar(255),

  embedding_model_version_id uuid,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (chunk_id, vector_store, vector_id)
);
CREATE INDEX IF NOT EXISTS idx_kb_embeddings_chunk ON kb.kb_embeddings(chunk_id);

-- Trigger: trg_kb_embeddings_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_kb_embeddings_touch
BEFORE UPDATE ON kb.kb_embeddings
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- -----------------------------------------------------------------------------
-- AI: Model registry + inference traces (auditability / reproducibility)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai.ai_models (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  name varchar(255) NOT NULL,
  model_type varchar(16) NOT NULL CHECK (model_type IN ('dkt','asag','rag','slm')),
  description text,
  is_active boolean NOT NULL DEFAULT TRUE,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (name, model_type)
);

-- Trigger: trg_ai_models_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_ai_models_touch
BEFORE UPDATE ON ai.ai_models
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS ai.ai_model_versions (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  model_id uuid NOT NULL REFERENCES ai.ai_models(id) ON DELETE CASCADE,
  version varchar(64) NOT NULL,
  artifact_uri varchar(1024),
  metrics jsonb,
  config jsonb,
  is_active boolean NOT NULL DEFAULT TRUE,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (model_id, version)
);
CREATE INDEX IF NOT EXISTS idx_ai_model_versions_model ON ai.ai_model_versions(model_id, created_at DESC);

-- Trigger: trg_ai_model_versions_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_ai_model_versions_touch
BEFORE UPDATE ON ai.ai_model_versions
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS ai.ai_inference_runs (
  trace_id varchar(64) PRIMARY KEY,  -- external trace key for reproducibility

  model_version_id uuid NOT NULL REFERENCES ai.ai_model_versions(id),

  school_id uuid REFERENCES lms.schools(id) ON DELETE SET NULL,
  student_id uuid REFERENCES lms.users(id) ON DELETE SET NULL,
  assessment_attempt_id uuid REFERENCES lms.assessment_attempts(id) ON DELETE SET NULL,
  attempt_answer_id uuid REFERENCES lms.attempt_answers(id) ON DELETE SET NULL,

  prompt_text text,
  context_json jsonb,

  rubric_scheme_id uuid REFERENCES lms.marking_schemes(id) ON DELETE SET NULL,
  rubric_scheme_version int,

  request_json jsonb NOT NULL,
  response_json jsonb NOT NULL,

  latency_ms int,
  created_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ai_inference_model_time  ON ai.ai_inference_runs(model_version_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_inference_school_time ON ai.ai_inference_runs(school_id, created_at DESC);

CREATE TABLE IF NOT EXISTS ai.ai_retrieval_traces (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  trace_id varchar(64) NOT NULL REFERENCES ai.ai_inference_runs(trace_id) ON DELETE CASCADE,
  kb_version_id uuid REFERENCES kb.kb_versions(id) ON DELETE SET NULL,

  chunk_ids uuid[],
  scores numeric(12,6)[],
  metadata jsonb,

  created_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ai_retrieval_trace_id ON ai.ai_retrieval_traces(trace_id);

-- -----------------------------------------------------------------------------
-- EDGE: Devices + sync control plane (outbox/inbox/checkpoints/conflicts/change-log)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS edge.edge_nodes (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  school_id uuid NOT NULL REFERENCES lms.schools(id) ON DELETE CASCADE,
  device_id varchar(128) NOT NULL,
  node_code varchar(100),
  status varchar(16) NOT NULL DEFAULT 'active' CHECK (status IN ('active','inactive','retired')),
  last_seen_at timestamptz,
  registered_at timestamptz NOT NULL DEFAULT NOW(),
  last_sync_at timestamptz,
  last_pull_at timestamptz,
  last_push_at timestamptz,
  software_version varchar(64),
  metadata jsonb,
  auth_key_hash varchar(255),
  sync_enabled boolean NOT NULL DEFAULT TRUE,
  tenant_code varchar(64),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 1,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (school_id, device_id)
);
CREATE INDEX IF NOT EXISTS idx_edge_nodes_school ON edge.edge_nodes(school_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_edge_nodes_node_code
  ON edge.edge_nodes(node_code) WHERE node_code IS NOT NULL AND deleted_at IS NULL;

-- Trigger: trg_edge_nodes_touch
-- Purpose: Updates sync metadata on insert/update.
CREATE TRIGGER trg_edge_nodes_touch
BEFORE INSERT OR UPDATE ON edge.edge_nodes
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS edge.edge_model_deployments (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  edge_node_id uuid NOT NULL REFERENCES edge.edge_nodes(id) ON DELETE CASCADE,
  model_version_id uuid NOT NULL REFERENCES ai.ai_model_versions(id),
  installed_at timestamptz NOT NULL DEFAULT NOW(),
  is_active boolean NOT NULL DEFAULT TRUE,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 1,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (edge_node_id, model_version_id)
);
CREATE INDEX IF NOT EXISTS idx_edge_deployments_node ON edge.edge_model_deployments(edge_node_id);

-- Trigger: trg_edge_model_deployments_touch
-- Purpose: Updates sync metadata on insert/update.
CREATE TRIGGER trg_edge_model_deployments_touch
BEFORE INSERT OR UPDATE ON edge.edge_model_deployments
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS edge.sync_outbox (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  edge_node_id uuid NOT NULL REFERENCES edge.edge_nodes(id) ON DELETE CASCADE,
  event_id uuid NOT NULL DEFAULT util.gen_uuid_v4(), -- idempotency key
  aggregate_type varchar(255) NOT NULL,
  aggregate_id uuid NOT NULL,
  operation varchar(16) NOT NULL CHECK (operation IN ('INSERT','UPDATE','DELETE')),
  entity_version bigint NOT NULL,
  payload jsonb NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','IN_PROGRESS','SYNCED','FAILED','CONFLICT')),
  attempts int NOT NULL DEFAULT 0,
  max_attempts int NOT NULL DEFAULT 10,
  batch_id uuid,
  locked_at timestamptz,
  locked_by varchar(128),
  next_retry_at timestamptz,
  last_error text,
  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),
  sent_at timestamptz,
  synced_at timestamptz,
  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 1,
  deleted_at timestamptz,

  UNIQUE (edge_node_id, event_id)
);
CREATE INDEX IF NOT EXISTS idx_sync_outbox_dispatch
  ON edge.sync_outbox(edge_node_id, status, next_retry_at, created_at);
CREATE INDEX IF NOT EXISTS idx_sync_outbox_aggregate
  ON edge.sync_outbox(aggregate_type, aggregate_id, entity_version);

-- Trigger: trg_sync_outbox_touch
-- Purpose: Updates sync metadata on insert/update.
CREATE TRIGGER trg_sync_outbox_touch
BEFORE INSERT OR UPDATE ON edge.sync_outbox
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS edge.sync_inbox (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  receiver_edge_node_id uuid NOT NULL REFERENCES edge.edge_nodes(id) ON DELETE CASCADE,
  cloud_change_id bigint NOT NULL,
  aggregate_type varchar(255) NOT NULL,
  aggregate_id uuid NOT NULL,
  operation varchar(16) NOT NULL CHECK (operation IN ('INSERT','UPDATE','DELETE')),
  entity_version bigint NOT NULL,
  payload jsonb NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'RECEIVED' CHECK (status IN ('RECEIVED','APPLIED','SKIPPED','FAILED','CONFLICT')),
  attempts int NOT NULL DEFAULT 0,
  error_message text,
  conflict_details jsonb,
  received_at timestamptz NOT NULL DEFAULT NOW(),
  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),
  processed_at timestamptz,
  applied_at timestamptz,
  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 1,
  deleted_at timestamptz,

  UNIQUE (receiver_edge_node_id, cloud_change_id)
);
CREATE INDEX IF NOT EXISTS idx_sync_inbox_status
  ON edge.sync_inbox(receiver_edge_node_id, status, received_at DESC);
CREATE INDEX IF NOT EXISTS idx_sync_inbox_aggregate
  ON edge.sync_inbox(aggregate_type, aggregate_id, entity_version);

-- Trigger: trg_sync_inbox_touch
-- Purpose: Updates sync metadata on insert/update.
CREATE TRIGGER trg_sync_inbox_touch
BEFORE INSERT OR UPDATE ON edge.sync_inbox
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS edge.sync_checkpoint (
  edge_node_id uuid PRIMARY KEY REFERENCES edge.edge_nodes(id) ON DELETE CASCADE,
  last_pulled_change_id bigint NOT NULL DEFAULT 0,
  last_successful_push_at timestamptz,
  last_successful_pull_at timestamptz,
  last_push_batch_id uuid,
  last_pull_batch_id uuid,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 1,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);

-- Trigger: trg_sync_checkpoint_touch
-- Purpose: Updates sync metadata on insert/update.
CREATE TRIGGER trg_sync_checkpoint_touch
BEFORE INSERT OR UPDATE ON edge.sync_checkpoint
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS edge.sync_conflict_log (
  id bigserial PRIMARY KEY,
  edge_node_id uuid NOT NULL REFERENCES edge.edge_nodes(id) ON DELETE CASCADE,
  conflict_scope varchar(64) NOT NULL,
  aggregate_type varchar(255) NOT NULL,
  aggregate_id uuid NOT NULL,
  local_version bigint,
  incoming_version bigint,
  resolved_version bigint,
  conflict_type varchar(64) NOT NULL,
  local_payload jsonb,
  incoming_payload jsonb,
  resolution_strategy varchar(64),
  resolved boolean NOT NULL DEFAULT FALSE,
  notes text,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 1,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_sync_conflict_log_edge
  ON edge.sync_conflict_log(edge_node_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_sync_conflict_log_aggregate
  ON edge.sync_conflict_log(aggregate_type, aggregate_id, created_at DESC);

-- Trigger: trg_sync_conflict_log_touch
-- Purpose: Updates sync metadata on insert/update.
CREATE TRIGGER trg_sync_conflict_log_touch
BEFORE INSERT OR UPDATE ON edge.sync_conflict_log
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS edge.sync_change_log (
  change_id bigserial PRIMARY KEY,
  source_edge_node_id uuid NOT NULL REFERENCES edge.edge_nodes(id) ON DELETE CASCADE,
  aggregate_type varchar(255) NOT NULL,
  aggregate_id uuid NOT NULL,
  operation varchar(16) NOT NULL CHECK (operation IN ('INSERT','UPDATE','DELETE')),
  entity_version bigint NOT NULL,
  payload jsonb NOT NULL,
  school_id uuid REFERENCES lms.schools(id) ON DELETE SET NULL,
  created_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_sync_change_log_school
  ON edge.sync_change_log(school_id, change_id);
CREATE INDEX IF NOT EXISTS idx_sync_change_log_source
  ON edge.sync_change_log(source_edge_node_id, change_id);

-- Constraints added after dependent tables exist
-- Purpose: Links interaction events to edge nodes (optional FK).
ALTER TABLE lms.interaction_events
  ADD CONSTRAINT fk_interactions_edge_node
  FOREIGN KEY (edge_node_id) REFERENCES edge.edge_nodes(id) ON DELETE SET NULL;

-- Purpose: Links embeddings to the AI model version that produced them (optional FK).
ALTER TABLE kb.kb_embeddings
  ADD CONSTRAINT fk_kb_embeddings_model_version
  FOREIGN KEY (embedding_model_version_id) REFERENCES ai.ai_model_versions(id) ON DELETE SET NULL;

-- -----------------------------------------------------------------------------
-- STUDENT SUPPORT: Parents/guardians + addresses + contacts + student documents
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lms.parents (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  first_name varchar(50) NOT NULL,
  last_name varchar(50) NOT NULL,
  email varchar(255),
  mobile varchar(50),
  alt_mobile varchar(50),
  occupation varchar(150),
  is_active boolean NOT NULL DEFAULT TRUE,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_parents_email  ON lms.parents(email);
CREATE INDEX IF NOT EXISTS idx_parents_mobile ON lms.parents(mobile);

-- Trigger: trg_parents_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_parents_touch
BEFORE UPDATE ON lms.parents
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.student_parents (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  student_id uuid NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,
  parent_id uuid NOT NULL REFERENCES lms.parents(id) ON DELETE CASCADE,
  relationship_code varchar(50) NOT NULL REFERENCES lookups.parent_relationship(code),
  is_primary_guardian boolean NOT NULL DEFAULT FALSE,
  notes text,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (student_id, parent_id, relationship_code)
);
CREATE INDEX IF NOT EXISTS idx_student_parents_student_id ON lms.student_parents(student_id);
CREATE INDEX IF NOT EXISTS idx_student_parents_parent_id  ON lms.student_parents(parent_id);

-- Trigger: trg_student_parents_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_student_parents_touch
BEFORE UPDATE ON lms.student_parents
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.addresses (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  line1 varchar(200) NOT NULL,
  line2 varchar(200),
  suburb varchar(100),
  city varchar(100),
  district varchar(100),
  province varchar(100),
  country_code varchar(10) DEFAULT 'ZW',
  postal_code varchar(20),
  latitude numeric(9,6),
  longitude numeric(9,6),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);

-- Trigger: trg_addresses_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_addresses_touch
BEFORE UPDATE ON lms.addresses
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.user_addresses (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  user_id uuid NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,
  address_id uuid NOT NULL REFERENCES lms.addresses(id) ON DELETE CASCADE,
  address_type_code varchar(50) NOT NULL REFERENCES lookups.address_type(code),
  is_primary boolean NOT NULL DEFAULT FALSE,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (user_id, address_id, address_type_code)
);
CREATE INDEX IF NOT EXISTS idx_user_addresses_user_id    ON lms.user_addresses(user_id);
CREATE INDEX IF NOT EXISTS idx_user_addresses_address_id ON lms.user_addresses(address_id);

-- Trigger: trg_user_addresses_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_user_addresses_touch
BEFORE UPDATE ON lms.user_addresses
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.user_contacts (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  user_id uuid NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,
  contact_channel_code varchar(50) NOT NULL REFERENCES lookups.contact_channel(code),
  value varchar(255) NOT NULL,
  is_primary boolean NOT NULL DEFAULT FALSE,
  notes text,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (user_id, contact_channel_code, value)
);
CREATE INDEX IF NOT EXISTS idx_user_contacts_user_id ON lms.user_contacts(user_id);

-- Trigger: trg_user_contacts_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_user_contacts_touch
BEFORE UPDATE ON lms.user_contacts
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.student_documents (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  student_id uuid NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,
  document_type_code varchar(50) NOT NULL REFERENCES lookups.document_type(code),
  file_name varchar(255) NOT NULL,
  storage_path varchar(500) NOT NULL,
  mime_type varchar(100),
  uploaded_at timestamptz NOT NULL DEFAULT NOW(),
  uploaded_by uuid REFERENCES lms.users(id),
  metadata jsonb,
  is_active boolean NOT NULL DEFAULT TRUE,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz
);
CREATE INDEX IF NOT EXISTS idx_student_documents_student_id ON lms.student_documents(student_id);
CREATE INDEX IF NOT EXISTS idx_student_documents_type       ON lms.student_documents(document_type_code);

-- -----------------------------------------------------------------------------
-- CHAT: Conversations + membership + messages
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lms.chats (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  school_id uuid NOT NULL REFERENCES lms.schools(id) ON DELETE CASCADE,
  chat_type varchar(16) NOT NULL DEFAULT 'direct' CHECK (chat_type IN ('direct','group')),
  title varchar(255),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_chats_school ON lms.chats(school_id);

-- Trigger: trg_chats_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_chats_touch
BEFORE UPDATE ON lms.chats
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.chat_members (
  chat_id uuid NOT NULL REFERENCES lms.chats(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,
  role varchar(16) NOT NULL DEFAULT 'member' CHECK (role IN ('member','admin')),
  joined_at timestamptz NOT NULL DEFAULT NOW(),
  PRIMARY KEY (chat_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_chat_members_user ON lms.chat_members(user_id);

CREATE TABLE IF NOT EXISTS lms.messages (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  school_id uuid NOT NULL REFERENCES lms.schools(id) ON DELETE CASCADE,
  chat_id uuid NOT NULL REFERENCES lms.chats(id) ON DELETE CASCADE,
  sender_id uuid NOT NULL REFERENCES lms.users(id),

  content text NOT NULL,
  ts timestamptz NOT NULL DEFAULT NOW(),
  is_read boolean NOT NULL DEFAULT FALSE,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_messages_chat_ts ON lms.messages(chat_id, ts);

-- Trigger: trg_messages_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_messages_touch
BEFORE UPDATE ON lms.messages
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- -----------------------------------------------------------------------------
-- AI TUTOR: Subject-scoped tutoring sessions + messages
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lms.ai_tutor_sessions (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  student_id uuid NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,
  subject_id uuid NOT NULL REFERENCES lms.subjects(id) ON DELETE CASCADE,

  status_code varchar(16) NOT NULL DEFAULT 'active'
    CHECK (status_code IN ('active','paused','archived')),
  last_message_at timestamptz,
  created_by uuid REFERENCES lms.users(id) ON DELETE SET NULL,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (student_id, subject_id)
);
CREATE INDEX IF NOT EXISTS idx_ai_tutor_sessions_student ON lms.ai_tutor_sessions(student_id);
CREATE INDEX IF NOT EXISTS idx_ai_tutor_sessions_subject ON lms.ai_tutor_sessions(subject_id);
CREATE INDEX IF NOT EXISTS idx_ai_tutor_sessions_status  ON lms.ai_tutor_sessions(status_code);

-- Trigger: trg_ai_tutor_sessions_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_ai_tutor_sessions_touch
BEFORE UPDATE ON lms.ai_tutor_sessions
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.ai_tutor_messages (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  session_id uuid NOT NULL REFERENCES lms.ai_tutor_sessions(id) ON DELETE CASCADE,
  sender_id uuid REFERENCES lms.users(id) ON DELETE SET NULL,

  sender_role varchar(16) NOT NULL DEFAULT 'student'
    CHECK (sender_role IN ('student','tutor','system')),
  content_type varchar(16) NOT NULL DEFAULT 'text'
    CHECK (content_type IN ('text','voice','content')),
  content text,
  transcript text,
  audio_url varchar(500),
  content_payload jsonb,
  ts timestamptz NOT NULL DEFAULT NOW(),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ai_tutor_messages_session_ts ON lms.ai_tutor_messages(session_id, ts);

-- Trigger: trg_ai_tutor_messages_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_ai_tutor_messages_touch
BEFORE UPDATE ON lms.ai_tutor_messages
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- -----------------------------------------------------------------------------
-- PEER STUDY: Student collaboration requests and participants
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lms.peer_study_requests (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  subject_id uuid NOT NULL REFERENCES lms.subjects(id) ON DELETE CASCADE,
  topic_id uuid REFERENCES lms.topics(id) ON DELETE SET NULL,

  topic_title varchar(255) NOT NULL,
  request_type varchar(16) NOT NULL DEFAULT 'need-help'
    CHECK (request_type IN ('need-help','offer-help','study-group')),
  note text NOT NULL,
  preferred_time timestamptz,
  status_code varchar(16) NOT NULL DEFAULT 'open'
    CHECK (status_code IN ('open','filled','closed','cancelled')),
  max_participants int NOT NULL DEFAULT 6 CHECK (max_participants > 0),

  created_by uuid NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_peer_study_requests_subject_status_created
  ON lms.peer_study_requests(subject_id, status_code, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_peer_study_requests_topic
  ON lms.peer_study_requests(topic_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_peer_study_requests_created_by
  ON lms.peer_study_requests(created_by, created_at DESC);

-- Trigger: trg_peer_study_requests_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_peer_study_requests_touch
BEFORE UPDATE ON lms.peer_study_requests
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.peer_study_request_members (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  request_id uuid NOT NULL REFERENCES lms.peer_study_requests(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,

  role_code varchar(16) NOT NULL DEFAULT 'member'
    CHECK (role_code IN ('host','member')),
  joined_at timestamptz NOT NULL DEFAULT NOW(),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_peer_study_request_members_active
  ON lms.peer_study_request_members(request_id, user_id)
  WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_peer_study_request_members_request
  ON lms.peer_study_request_members(request_id, joined_at);
CREATE INDEX IF NOT EXISTS idx_peer_study_request_members_user
  ON lms.peer_study_request_members(user_id, joined_at DESC);

-- Trigger: trg_peer_study_request_members_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_peer_study_request_members_touch
BEFORE UPDATE ON lms.peer_study_request_members
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- -----------------------------------------------------------------------------
-- NOTIFICATIONS: In-app events for users (grade posted, message received, etc.)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lms.notifications (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  school_id uuid NOT NULL REFERENCES lms.schools(id) ON DELETE CASCADE,
  recipient_id uuid NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,

  notif_type varchar(32) NOT NULL
    CHECK (notif_type IN (
      'assignment_graded','assignment_submitted','plan_assigned','message_received',
      'assessment_assigned','assessment_published','assessment_deadline_changed','assessment_submitted',
      'development_plan_assigned','development_plan_updated','development_plan_published','development_plan_unpublished',
      'resource_published'
    )),

  title varchar(255) NOT NULL,
  message text NOT NULL,
  data jsonb,

  is_read boolean NOT NULL DEFAULT FALSE,
  read_at timestamptz,

  priority varchar(8) NOT NULL DEFAULT 'medium' CHECK (priority IN ('low','medium','high','urgent')),
  expires_at timestamptz,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient
  ON lms.notifications(school_id, recipient_id, is_read, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_read_created
  ON lms.notifications(recipient_id, is_read, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_ttl ON lms.notifications(expires_at);

-- Trigger: trg_notifications_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_notifications_touch
BEFORE UPDATE ON lms.notifications
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- -----------------------------------------------------------------------------
-- CALENDAR: Schedules for classes/events/exams/holidays
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lms.calendar_events (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  school_id uuid NOT NULL REFERENCES lms.schools(id) ON DELETE CASCADE,

  title varchar(255) NOT NULL,
  description text,
  start_time timestamptz NOT NULL,
  end_time timestamptz,
  all_day boolean NOT NULL DEFAULT FALSE,

  event_type varchar(32) NOT NULL CHECK
    (event_type IN ('lesson','lab','assignment_due','exam','meeting','office_hours','holiday','workshop','seminar','presentation','project_due','quiz')),

  class_id uuid REFERENCES lms.classes(id) ON DELETE SET NULL,
  subject_id uuid REFERENCES lms.subjects(id) ON DELETE SET NULL,

  location varchar(255),
  attendees text[],
  recurring jsonb,
  reminders jsonb,

  created_by uuid NOT NULL REFERENCES lms.users(id),
  is_public boolean NOT NULL DEFAULT FALSE,
  status varchar(16) NOT NULL DEFAULT 'active' CHECK (status IN ('active','cancelled','completed')),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_calendar_start ON lms.calendar_events(school_id, start_time, end_time);
CREATE INDEX IF NOT EXISTS idx_calendar_class ON lms.calendar_events(class_id, start_time);
CREATE INDEX IF NOT EXISTS idx_calendar_subject_class_start
  ON lms.calendar_events(subject_id, class_id, start_time);

-- Trigger: trg_calendar_events_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_calendar_events_touch
BEFORE UPDATE ON lms.calendar_events
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- -----------------------------------------------------------------------------
-- PLANS: Learning plans and their ordered steps + dashboards
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lms.plans (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  subject_id uuid NOT NULL REFERENCES lms.subjects(id) ON DELETE CASCADE,
  name varchar(200) NOT NULL,
  description text NOT NULL,

  progress numeric(5,2) NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
  potential_overall numeric(5,2) NOT NULL CHECK (potential_overall BETWEEN 0 AND 100),
  eta_days int NOT NULL CHECK (eta_days > 0),
  performance varchar(128) NOT NULL,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);

-- Trigger: trg_plans_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_plans_touch
BEFORE UPDATE ON lms.plans
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.plan_steps (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  plan_id uuid NOT NULL REFERENCES lms.plans(id) ON DELETE CASCADE,
  title varchar(200) NOT NULL,
  step_type varchar(32) NOT NULL CHECK (step_type IN ('video','document','assessment','discussion')),
  content text,
  link varchar(1024),
  step_order int NOT NULL,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (plan_id, step_order)
);
ALTER TABLE IF EXISTS lms.plan_steps ADD COLUMN IF NOT EXISTS content text;

-- Trigger: trg_plan_steps_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_plan_steps_touch
BEFORE UPDATE ON lms.plan_steps
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.plan_skills (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  plan_id uuid NOT NULL REFERENCES lms.plans(id) ON DELETE CASCADE,
  skill_id uuid REFERENCES lms.skills(id) ON DELETE SET NULL,

  name varchar(200) NOT NULL,
  score numeric(5,2) CHECK (score BETWEEN 0 AND 100),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_plan_skills_plan  ON lms.plan_skills(plan_id);
CREATE INDEX IF NOT EXISTS idx_plan_skills_skill ON lms.plan_skills(skill_id);

-- Trigger: trg_plan_skills_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_plan_skills_touch
BEFORE UPDATE ON lms.plan_skills
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

CREATE TABLE IF NOT EXISTS lms.plan_subskills (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  plan_skill_id uuid NOT NULL REFERENCES lms.plan_skills(id) ON DELETE CASCADE,

  name varchar(200) NOT NULL,
  score numeric(5,2) CHECK (score BETWEEN 0 AND 100),
  color varchar(16) NOT NULL DEFAULT 'yellow'
    CHECK (color IN ('yellow','cyan','blue','green','red')),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_plan_subskills_plan_skill ON lms.plan_subskills(plan_skill_id);

-- Trigger: trg_plan_subskills_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_plan_subskills_touch
BEFORE UPDATE ON lms.plan_subskills
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- Table: lms.student_plans
-- Purpose: Assigns plans to students per subject; supports history + “current plan”.
CREATE TABLE IF NOT EXISTS lms.student_plans (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  student_id uuid NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,
  plan_id uuid NOT NULL REFERENCES lms.plans(id) ON DELETE CASCADE,
  subject_id uuid NOT NULL REFERENCES lms.subjects(id) ON DELETE CASCADE,

  start_date timestamptz NOT NULL DEFAULT NOW(),
  current_progress numeric(5,2) NOT NULL DEFAULT 0 CHECK (current_progress BETWEEN 0 AND 100),
  active_step_id uuid REFERENCES lms.plan_steps(id) ON DELETE SET NULL,
  completed_step_ids jsonb NOT NULL DEFAULT '[]'::jsonb,

  status varchar(16) NOT NULL DEFAULT 'on_hold' CHECK (status IN ('active','completed','on_hold','cancelled')),
  is_current boolean NOT NULL DEFAULT FALSE,
  completion_date timestamptz,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);

-- Index: ux_student_plans_one_current
-- Purpose: Enforces only one “current” plan per (student, subject).
CREATE UNIQUE INDEX IF NOT EXISTS ux_student_plans_one_current
  ON lms.student_plans(student_id, subject_id)
  WHERE is_current = TRUE;

-- Trigger: trg_student_plans_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_student_plans_touch
BEFORE UPDATE ON lms.student_plans
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- Table: lms.student_activity_days
-- Purpose: Tracks daily learner activity for streak computation across devices/sessions.
CREATE TABLE IF NOT EXISTS lms.student_activity_days (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  student_id uuid NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,
  activity_date date NOT NULL,
  activity_count int NOT NULL DEFAULT 1 CHECK (activity_count >= 0),
  last_activity_at timestamptz NOT NULL DEFAULT NOW(),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_student_activity_days_student_date
  ON lms.student_activity_days(student_id, activity_date)
  WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_student_activity_days_student_date_desc
  ON lms.student_activity_days(student_id, activity_date DESC);

-- Trigger: trg_student_activity_days_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_student_activity_days_touch
BEFORE UPDATE ON lms.student_activity_days
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- Table: lms.student_profiles
-- Purpose: Student-specific profile attributes without polluting the base user table.
CREATE TABLE IF NOT EXISTS lms.student_profiles (
  user_id uuid PRIMARY KEY REFERENCES lms.users(id) ON DELETE CASCADE,

  grade_level varchar(50),
  exam_board_code varchar(50) REFERENCES lookups.exam_board(code),

  overall numeric(5,2) CHECK (overall BETWEEN 0 AND 100),
  engagement varchar(64),
  strength text,
  performance text,

  active_student_plan_id uuid REFERENCES lms.student_plans(id) ON DELETE SET NULL,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);

-- Trigger: trg_student_profiles_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_student_profiles_touch
BEFORE UPDATE ON lms.student_profiles
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- -----------------------------------------------------------------------------
-- RETEACH CARDS: Topic-level remediation guidance + affected learners
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lms.reteach_cards (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  subject_id uuid NOT NULL REFERENCES lms.subjects(id) ON DELETE CASCADE,
  topic_id uuid REFERENCES lms.topics(id) ON DELETE SET NULL,

  title varchar(200) NOT NULL,
  issue_summary text,
  recommended_actions text,

  priority_code varchar(16) NOT NULL DEFAULT 'medium'
    CHECK (priority_code IN ('low','medium','high')),
  status_code varchar(16) NOT NULL DEFAULT 'draft'
    CHECK (status_code IN ('draft','active','resolved','archived')),

  affected_student_ids jsonb,
  created_by uuid REFERENCES lms.users(id) ON DELETE SET NULL,

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reteach_cards_subject  ON lms.reteach_cards(subject_id);
CREATE INDEX IF NOT EXISTS idx_reteach_cards_topic    ON lms.reteach_cards(topic_id);
CREATE INDEX IF NOT EXISTS idx_reteach_cards_priority ON lms.reteach_cards(priority_code);
CREATE INDEX IF NOT EXISTS idx_reteach_cards_status   ON lms.reteach_cards(status_code);

-- Trigger: trg_reteach_cards_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_reteach_cards_touch
BEFORE UPDATE ON lms.reteach_cards
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- Table: lms.student_attributes
-- Purpose: Per-student per-skill scores used by analytics and mastery dashboards.
CREATE TABLE IF NOT EXISTS lms.student_attributes (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  student_id uuid NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,
  skill_id uuid NOT NULL REFERENCES lms.skills(id) ON DELETE CASCADE,

  current_score numeric(5,2) NOT NULL CHECK (current_score BETWEEN 0 AND 100),
  potential_score numeric(5,2) NOT NULL CHECK (potential_score BETWEEN 0 AND 100),
  last_assessed timestamptz NOT NULL DEFAULT NOW(),

  origin_node_id uuid,
  sync_version bigint NOT NULL DEFAULT 0,
  deleted_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT NOW(),
  updated_at timestamptz NOT NULL DEFAULT NOW(),

  UNIQUE (student_id, skill_id)
);
CREATE INDEX IF NOT EXISTS idx_student_attributes_student ON lms.student_attributes(student_id);

-- Trigger: trg_student_attributes_touch
-- Purpose: Updates updated_at and increments sync_version on update.
CREATE TRIGGER trg_student_attributes_touch
BEFORE UPDATE ON lms.student_attributes
FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

-- -----------------------------------------------------------------------------
-- AUDIT: Append-only immutable log for analytics + compliance
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit.event_log (
  id uuid PRIMARY KEY DEFAULT util.gen_uuid_v4(),
  school_id uuid REFERENCES lms.schools(id) ON DELETE SET NULL,
  actor_id uuid REFERENCES lms.users(id) ON DELETE SET NULL,

  event_type varchar(64) NOT NULL,
  entity_type varchar(64),
  entity_id varchar(64),
  payload jsonb,

  created_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_event_log_school_time ON audit.event_log(school_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_event_log_actor_time  ON audit.event_log(actor_id, created_at DESC);

-- -----------------------------------------------------------------------------
-- MIGRATIONS: additive adjustments for existing databases
-- -----------------------------------------------------------------------------
ALTER TABLE lms.resources
  ADD COLUMN IF NOT EXISTS content_type varchar(64),
  ADD COLUMN IF NOT EXISTS content_body text,
  ADD COLUMN IF NOT EXISTS publish_at timestamptz,
  ADD COLUMN IF NOT EXISTS tags text[],
  ADD COLUMN IF NOT EXISTS storage_key varchar(512),
  ADD COLUMN IF NOT EXISTS storage_path varchar(1024),
  ADD COLUMN IF NOT EXISTS display_order int NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS status varchar(16) NOT NULL DEFAULT 'active';

ALTER TABLE lms.resources
  ALTER COLUMN status DROP DEFAULT,
  ALTER COLUMN status SET DEFAULT 'active';
