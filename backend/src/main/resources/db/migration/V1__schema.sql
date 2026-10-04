-- BookForward core schema. Schema changes must be added as new versioned migrations.
CREATE TABLE users (
  id uuid PRIMARY KEY,
  email varchar(255) NOT NULL UNIQUE,
  password_hash varchar(100) NOT NULL,
  display_name varchar(100) NOT NULL,
  status varchar(20) NOT NULL,
  token_version int NOT NULL DEFAULT 0,
  last_seen_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE user_roles (
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role varchar(20) NOT NULL,
  PRIMARY KEY (user_id, role)
);
CREATE TABLE profiles (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  bio varchar(500), institution varchar(150), city varchar(100),
  academic_level varchar(20), board varchar(50), target_exam varchar(100),
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE categories (
  id uuid PRIMARY KEY,
  name varchar(100) NOT NULL UNIQUE,
  slug varchar(100) NOT NULL UNIQUE,
  description varchar(255),
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE storage_objects (
  id uuid PRIMARY KEY,
  owner_id uuid NOT NULL REFERENCES users(id),
  object_key varchar(255) NOT NULL UNIQUE,
  provider varchar(30) NOT NULL,
  content_type varchar(100) NOT NULL,
  size_bytes bigint NOT NULL,
  checksum varchar(64) NOT NULL,
  state varchar(20) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_storage_owner ON storage_objects(owner_id);
CREATE TABLE listings (
  id uuid PRIMARY KEY,
  seller_id uuid NOT NULL REFERENCES users(id),
  category_id uuid NOT NULL REFERENCES categories(id),
  title varchar(200) NOT NULL,
  author varchar(150) NOT NULL,
  publisher varchar(150),
  isbn varchar(20),
  description varchar(4000),
  academic_level varchar(20) NOT NULL,
  board varchar(50),
  ncert_applicable boolean NOT NULL DEFAULT false,
  book_condition varchar(20) NOT NULL,
  price numeric(10,2) NOT NULL CHECK (price >= 0),
  status varchar(20) NOT NULL,
  moderation_reason varchar(500),
  published_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_listings_seller ON listings(seller_id);
CREATE INDEX idx_listings_category ON listings(category_id);
CREATE INDEX idx_listings_status_created ON listings(status, created_at DESC);
CREATE INDEX idx_listings_level ON listings(academic_level);
CREATE INDEX idx_listings_price ON listings(price);
CREATE INDEX idx_listings_title_lower ON listings(lower(title));
CREATE TABLE listing_images (
  id uuid PRIMARY KEY,
  listing_id uuid NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
  storage_object_id uuid NOT NULL REFERENCES storage_objects(id),
  image_type varchar(20) NOT NULL,
  display_order int NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_listing_images_listing ON listing_images(listing_id);
CREATE TABLE saved_books (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  listing_id uuid NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_saved_user_listing UNIQUE (user_id, listing_id)
);
CREATE TABLE purchase_requests (
  id uuid PRIMARY KEY,
  listing_id uuid NOT NULL REFERENCES listings(id),
  buyer_id uuid NOT NULL REFERENCES users(id),
  seller_id uuid NOT NULL REFERENCES users(id),
  status varchar(20) NOT NULL,
  message varchar(500),
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_requests_buyer ON purchase_requests(buyer_id);
CREATE INDEX idx_requests_seller ON purchase_requests(seller_id);
CREATE INDEX idx_requests_listing_status ON purchase_requests(listing_id, status);
CREATE TABLE orders (
  id uuid PRIMARY KEY,
  purchase_request_id uuid NOT NULL UNIQUE REFERENCES purchase_requests(id),
  listing_id uuid NOT NULL REFERENCES listings(id),
  buyer_id uuid NOT NULL REFERENCES users(id),
  seller_id uuid NOT NULL REFERENCES users(id),
  status varchar(20) NOT NULL,
  total_amount numeric(10,2) NOT NULL,
  currency varchar(3) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_orders_buyer ON orders(buyer_id);
CREATE INDEX idx_orders_seller ON orders(seller_id);
CREATE TABLE order_status_history (
  id uuid PRIMARY KEY,
  order_id uuid NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  old_status varchar(20),
  new_status varchar(20) NOT NULL,
  actor_id uuid NOT NULL REFERENCES users(id),
  reason varchar(500),
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_order_history_order ON order_status_history(order_id);
CREATE TABLE payments (
  id uuid PRIMARY KEY,
  order_id uuid NOT NULL REFERENCES orders(id),
  provider varchar(30) NOT NULL,
  provider_reference varchar(100),
  status varchar(20) NOT NULL,
  amount numeric(10,2) NOT NULL,
  currency varchar(3) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_payments_order ON payments(order_id);
CREATE TABLE conversations (
  id uuid PRIMARY KEY,
  listing_id uuid REFERENCES listings(id) ON DELETE SET NULL,
  participant_key varchar(120) NOT NULL UNIQUE,
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE conversation_members (
  id uuid PRIMARY KEY,
  conversation_id uuid NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  last_read_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_member UNIQUE (conversation_id, user_id)
);
CREATE INDEX idx_members_user ON conversation_members(user_id);
CREATE TABLE messages (
  id uuid PRIMARY KEY,
  conversation_id uuid NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
  sender_id uuid NOT NULL REFERENCES users(id),
  content varchar(2000) NOT NULL,
  message_type varchar(20) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_messages_conv_created ON messages(conversation_id, created_at DESC);
CREATE TABLE notifications (
  id uuid PRIMARY KEY,
  recipient_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  notification_type varchar(40) NOT NULL,
  title varchar(150) NOT NULL,
  body varchar(500),
  link varchar(255),
  seen boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_recipient ON notifications(recipient_id, created_at DESC);
CREATE TABLE reviews (
  id uuid PRIMARY KEY,
  order_id uuid NOT NULL REFERENCES orders(id),
  reviewer_id uuid NOT NULL REFERENCES users(id),
  reviewee_id uuid NOT NULL REFERENCES users(id),
  listing_id uuid NOT NULL REFERENCES listings(id),
  rating int NOT NULL CHECK (rating BETWEEN 1 AND 5),
  comment varchar(1000),
  status varchar(20) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_review_order_reviewer UNIQUE (order_id, reviewer_id)
);
CREATE INDEX idx_reviews_listing ON reviews(listing_id);
CREATE INDEX idx_reviews_reviewee ON reviews(reviewee_id);
CREATE TABLE reports (
  id uuid PRIMARY KEY,
  reporter_id uuid NOT NULL REFERENCES users(id),
  target_type varchar(20) NOT NULL,
  target_id uuid NOT NULL,
  reason varchar(50) NOT NULL,
  details varchar(1000),
  status varchar(20) NOT NULL,
  resolution varchar(500),
  resolved_by_id uuid REFERENCES users(id),
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_reports_status ON reports(status, created_at DESC);
CREATE TABLE moderation_actions (
  id uuid PRIMARY KEY,
  moderator_id uuid NOT NULL REFERENCES users(id),
  target_type varchar(20) NOT NULL,
  target_id uuid NOT NULL,
  action varchar(30) NOT NULL,
  reason varchar(500),
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE audit_logs (
  id uuid PRIMARY KEY,
  actor_id uuid REFERENCES users(id),
  action varchar(60) NOT NULL,
  resource_type varchar(40) NOT NULL,
  resource_id varchar(64),
  metadata varchar(2000),
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_created ON audit_logs(created_at DESC);
