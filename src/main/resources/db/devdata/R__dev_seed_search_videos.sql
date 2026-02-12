-- Dev seed data: Search index for test videos
-- Mirrors video-service seed data so videos appear in search results
-- Note: In production, this table is populated via SQS events from moderation-service

INSERT INTO search.search_videos (
    id, youtube_id, title, description, channel_name,
    amendments, participants,
    primary_location_id, primary_location_name, primary_location_city, primary_location_state,
    primary_location_lat, primary_location_lng
) VALUES
    -- San Francisco Bay Area (Videos 1-5)
    ('10000000-0000-0000-0000-000000000001', 'RngL8_3k0C0',
     'Northern California Government Building Audit',
     'First Amendment audit of a government building in Northern California.',
     'Phil',
     ARRAY['FIRST']::VARCHAR[], ARRAY['POLICE', 'GOVERNMENT']::VARCHAR[],
     '20000000-0000-0000-0000-000000000001', 'San Francisco City Hall', 'San Francisco', 'CA',
     37.7793, -122.4193),

    ('10000000-0000-0000-0000-000000000002', 'nQRpazbSRf4',
     'East Lansing Police Department Audit Analysis',
     'Audit the Audit analysis of ELPD First Amendment audit incident.',
     'Audit the Audit',
     ARRAY['FIRST', 'FOURTH']::VARCHAR[], ARRAY['POLICE']::VARCHAR[],
     '20000000-0000-0000-0000-000000000002', 'Oakland Federal Building', 'Oakland', 'CA',
     37.8044, -122.2712),

    ('10000000-0000-0000-0000-000000000003', 'ULjtPKeh9Co',
     '61st Precinct Brooklyn - Arrest During Audit',
     'SeanPaul Reyes arrested while recording in the lobby of the 61st Precinct in Brooklyn.',
     'Long Island Audit',
     ARRAY['FIRST', 'FOURTH']::VARCHAR[], ARRAY['POLICE']::VARCHAR[],
     '20000000-0000-0000-0000-000000000003', 'San Jose Police HQ', 'San Jose', 'CA',
     37.3382, -121.8863),

    ('10000000-0000-0000-0000-000000000004', 'AJi0LgnoIJA',
     'Utica Michigan Police Confrontation',
     'Steve Jones confronted by Detective Sergeant during First Amendment audit.',
     'Fricn Media',
     ARRAY['FIRST', 'FOURTH']::VARCHAR[], ARRAY['POLICE']::VARCHAR[],
     '20000000-0000-0000-0000-000000000004', 'Fremont City Hall', 'Fremont', 'CA',
     37.5485, -121.9886),

    ('10000000-0000-0000-0000-000000000005', 'OdsTAYnC8Kc',
     'Pocahontas City Hall Audit',
     'First Amendment audit at Pocahontas, Arkansas city hall.',
     'The Random Patriot',
     ARRAY['FIRST']::VARCHAR[], ARRAY['GOVERNMENT']::VARCHAR[],
     '20000000-0000-0000-0000-000000000005', 'Berkeley Post Office', 'Berkeley', 'CA',
     37.8716, -122.2727),

    -- Scattered across USA (Videos 6-10)
    ('10000000-0000-0000-0000-000000000006', '-kNacBPsNxo',
     'San Antonio Strip Mall Encounter',
     'First Amendment audit encounter at a San Antonio strip mall.',
     'Mexican Padilla',
     ARRAY['FIRST']::VARCHAR[], ARRAY['POLICE', 'BUSINESS']::VARCHAR[],
     '20000000-0000-0000-0000-000000000006', 'San Antonio Strip Mall', 'San Antonio', 'TX',
     29.4241, -98.4936),

    ('10000000-0000-0000-0000-000000000007', 'IX_8Epjcp54',
     'Leon Valley Police Chief Press Conference',
     'Coverage of Leon Valley Police Department press conference.',
     'News Now Houston',
     ARRAY['FIRST']::VARCHAR[], ARRAY['POLICE', 'GOVERNMENT']::VARCHAR[],
     '20000000-0000-0000-0000-000000000007', 'Leon Valley Police Department', 'Leon Valley', 'TX',
     29.4952, -98.6136),

    ('10000000-0000-0000-0000-000000000008', 'hkhrXPur4ws',
     'Silverthorne Post Office Audit',
     'First Amendment audit at Silverthorne, Colorado post office that led to settlement.',
     'Amagansett Press',
     ARRAY['FIRST']::VARCHAR[], ARRAY['GOVERNMENT']::VARCHAR[],
     '20000000-0000-0000-0000-000000000008', 'Silverthorne Post Office', 'Silverthorne', 'CO',
     39.6336, -106.0753),

    ('10000000-0000-0000-0000-000000000009', 'QgkT4epLRcw',
     'East Lansing PD Incident',
     'Direct footage from East Lansing Police Department First Amendment audit.',
     'Livingston Audits',
     ARRAY['FIRST']::VARCHAR[], ARRAY['POLICE']::VARCHAR[],
     '20000000-0000-0000-0000-000000000009', 'East Lansing Police Department', 'East Lansing', 'MI',
     42.7370, -84.4839),

    ('10000000-0000-0000-0000-000000000010', 'FwvZCn0uLiw',
     'Pocahontas City Hall - Uncut Footage',
     'Full unedited footage from Pocahontas, Arkansas city hall audit.',
     'The Random Patriot',
     ARRAY['FIRST']::VARCHAR[], ARRAY['GOVERNMENT']::VARCHAR[],
     '20000000-0000-0000-0000-000000000010', 'Pocahontas City Hall', 'Pocahontas', 'AR',
     36.2612, -90.9712)
ON CONFLICT (id) DO UPDATE SET
    youtube_id = EXCLUDED.youtube_id,
    title = EXCLUDED.title,
    description = EXCLUDED.description,
    channel_name = EXCLUDED.channel_name,
    amendments = EXCLUDED.amendments,
    participants = EXCLUDED.participants,
    primary_location_id = EXCLUDED.primary_location_id,
    primary_location_name = EXCLUDED.primary_location_name,
    primary_location_city = EXCLUDED.primary_location_city,
    primary_location_state = EXCLUDED.primary_location_state,
    primary_location_lat = EXCLUDED.primary_location_lat,
    primary_location_lng = EXCLUDED.primary_location_lng,
    indexed_at = NOW();
