-- Create search schema
CREATE SCHEMA IF NOT EXISTS search;

-- Denormalized search_videos table
CREATE TABLE search.search_videos (
    id UUID PRIMARY KEY,
    youtube_id VARCHAR(11) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    thumbnail_url VARCHAR(500),
    duration_seconds INTEGER,
    channel_id VARCHAR(50),
    channel_name VARCHAR(255),
    video_date DATE,
    amendments VARCHAR(20)[] NOT NULL DEFAULT '{}',
    participants VARCHAR(20)[] NOT NULL DEFAULT '{}',
    primary_location_id UUID,
    primary_location_name VARCHAR(200),
    primary_location_city VARCHAR(100),
    primary_location_state VARCHAR(50),
    primary_location_lat DOUBLE PRECISION,
    primary_location_lng DOUBLE PRECISION,
    indexed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    search_vector TSVECTOR
);

-- Trigger to maintain search_vector with weighted ranking
-- Weights: title (A), channel_name (B), description (C)
CREATE OR REPLACE FUNCTION search.update_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.channel_name, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER search_videos_vector_trigger
    BEFORE INSERT OR UPDATE OF title, channel_name, description ON search.search_videos
    FOR EACH ROW EXECUTE FUNCTION search.update_search_vector();

-- Indexes
CREATE INDEX idx_search_videos_youtube_id ON search.search_videos(youtube_id);
CREATE INDEX idx_search_videos_channel_id ON search.search_videos(channel_id);
CREATE INDEX idx_search_videos_video_date ON search.search_videos(video_date);
CREATE INDEX idx_search_videos_amendments ON search.search_videos USING GIN(amendments);
CREATE INDEX idx_search_videos_participants ON search.search_videos USING GIN(participants);
CREATE INDEX idx_search_videos_state ON search.search_videos(primary_location_state);
CREATE INDEX idx_search_videos_search_vector ON search.search_videos USING GIN(search_vector);
