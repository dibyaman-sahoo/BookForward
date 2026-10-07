-- Seller location on listings; author becomes optional.
ALTER TABLE listings ALTER COLUMN author DROP NOT NULL;
ALTER TABLE listings
  ADD COLUMN address_line varchar(200),
  ADD COLUMN area varchar(150),
  ADD COLUMN city varchar(100),
  ADD COLUMN state varchar(100),
  ADD COLUMN postal_code varchar(20),
  ADD COLUMN latitude double precision,
  ADD COLUMN longitude double precision;
CREATE INDEX idx_listings_city ON listings(lower(city));
CREATE INDEX idx_listings_lat_lon ON listings(latitude, longitude);
