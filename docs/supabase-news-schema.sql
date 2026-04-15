-- Solea Pixel — live news for Netlify `news-feed` / `news-admin`.
-- Run in Supabase SQL editor (once per project).

create table if not exists public.news_posts (
  id uuid primary key default gen_random_uuid(),
  sort_order integer not null default 0,
  title text not null default '',
  body text not null default '',
  is_published boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists news_posts_published_sort
  on public.news_posts (is_published, sort_order desc, updated_at desc);

alter table public.news_posts enable row level security;

grant usage on schema public to anon, authenticated;
grant select on public.news_posts to anon, authenticated;

-- Public launcher + site: read only published rows (anon key).
drop policy if exists "news_posts_public_select_published" on public.news_posts;
create policy "news_posts_public_select_published"
  on public.news_posts
  for select
  to anon, authenticated
  using (is_published = true);

-- Writes go through Netlify `news-admin` with the service role (bypasses RLS).
-- Optional hardening: revoke direct writes from anon/authenticated on this table
-- if you expose the table in PostgREST (default Supabase API still lists it;
-- without policies insert/update/delete are denied for anon).

comment on table public.news_posts is 'Solea Pixel news cards; segments built in news-feed Netlify function.';
