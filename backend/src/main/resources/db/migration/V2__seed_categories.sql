-- Reference data (not demo data): category master list.
INSERT INTO categories (id, name, slug) VALUES
 (gen_random_uuid(), 'Mathematics', 'mathematics'),
 (gen_random_uuid(), 'Physics', 'physics'),
 (gen_random_uuid(), 'Chemistry', 'chemistry'),
 (gen_random_uuid(), 'Biology', 'biology'),
 (gen_random_uuid(), 'Computer Science', 'computer-science'),
 (gen_random_uuid(), 'Engineering', 'engineering'),
 (gen_random_uuid(), 'Medical & Health', 'medical-health'),
 (gen_random_uuid(), 'Commerce & Accounting', 'commerce-accounting'),
 (gen_random_uuid(), 'Humanities & Social Science', 'humanities'),
 (gen_random_uuid(), 'Languages & Literature', 'languages-literature'),
 (gen_random_uuid(), 'Competitive Exams', 'competitive-exams'),
 (gen_random_uuid(), 'Reference & Others', 'reference-others');
