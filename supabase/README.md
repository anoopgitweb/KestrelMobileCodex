# Supabase setup

1. Create a Supabase project.
2. Open **SQL Editor** and run `migrations/20260920_user_profiles_and_preferences.sql`.
3. In the app login screen, open **Supabase Project URL & Anon Key Config** and enter the project URL and publishable/anon key.
4. Create users in **Authentication > Users**. The app never stores a user's password.

Profiles and preferences are protected by Row Level Security. An authenticated user can only read and change rows whose ID matches the ID in their Supabase access token.

The four-digit PIN is a local quick-unlock code. It is salted and hashed on the phone and is never uploaded to Supabase. A new phone or a full sign-out requires the Supabase email and password again.
