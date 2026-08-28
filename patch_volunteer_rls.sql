-- Add Volunteer and Admin update policy for blood_requests
CREATE POLICY "Volunteers and Admins can update requests" ON "public"."blood_requests"
  FOR UPDATE USING (
    (SELECT role FROM user_accounts WHERE email = auth.email()) IN ('Admin', 'Volunteer', 'NSS Volunteer', 'NCC Volunteer', 'YRC Volunteer')
    OR auth.role() = 'service_role'
  );
