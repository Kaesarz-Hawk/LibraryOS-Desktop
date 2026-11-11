-- ============================================================================
-- LibraryOS: Phase 7 — Friends & Community Seeding
-- Circulation Desk, Dashboard Metrics, Fines, Payments, Reservations,
-- Members Directory, Institutional Suppliers, Reviews & Reading History
-- Compatibility: MySQL 8.0+ and H2 Database (MySQL Mode)
-- ============================================================================
-- 1. Suppliers (Sadia Afrin, Sojibur Rahman, Pushpo / Sohan Ibn Sahid)
INSERT INTO
  suppliers (
    id,
    name,
    contact_person,
    phone,
    email,
    address,
    is_active,
    created_at
  )
VALUES
  (
    4,
    'সাদিয়া বুক এজেন্সি (Sadia Afrin Book Agency)',
    'Sadia Afrin',
    '+8801711234501',
    'sadia.afrin@booksupply.bd',
    'Nilkhet Central Book Market, Dhaka',
    TRUE,
    CURRENT_TIMESTAMP
  ),
  (
    5,
    'রহমান সজীবুর বুক ডিস্ট্রিবিউটরস (Sojibur Book Distributors)',
    'Sojibur Rahman (রহমান সজীবুর)',
    '+8801811234502',
    'sojibur.rahman@dhakabooks.com',
    'Bangla Bazar, Dhaka-1100',
    TRUE,
    CURRENT_TIMESTAMP
  ),
  (
    6,
    'পুষ্প একাডেমি অ্যান্ড পাবলিকেশন্স (Pushpo Academic Supply)',
    'Sohan Ibn Sahid (Pushpo)',
    '+8801911234503',
    'pushpo.sahid@pushpobooks.com',
    'Mirpur-10, Dhaka-1216',
    TRUE,
    CURRENT_TIMESTAMP
  );

-- 2. Members Directory (44 Friends, IDs 4 to 47)
INSERT INTO
  members (
    id,
    membership_number,
    member_type_id,
    full_name_en,
    full_name_bn,
    institutional_id,
    department,
    program,
    semester_or_session,
    phone,
    email,
    address,
    membership_start_date,
    membership_expiry_date,
    status
  )
VALUES
  (
    4,
    'LIB-2026-0004',
    1,
    'Kawsar Hoque',
    'কাওসার হক',
    'CSE-2022-001',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000001',
    'kawsar.hoque@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    5,
    'LIB-2026-0005',
    1,
    'Sarah',
    'সারাহ',
    'CSE-2022-002',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000002',
    'sarah.rahman@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    6,
    'LIB-2026-0006',
    1,
    'Risalat Bepari Rakiba',
    'রিসালাত বেপারী রাকিবা',
    'CSE-2022-003',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000003',
    'risalat.bepari@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    7,
    'LIB-2026-0007',
    1,
    'Fariha Lubaba',
    'ফারিহা লুবাবা',
    'SWE-2022-010',
    'Software Engineering',
    'B.Sc in SWE',
    'Spring 2026',
    '+8801711000004',
    'fariha.lubaba@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    8,
    'LIB-2026-0008',
    1,
    'Abir Anjum Ankon',
    'আবির আনজুম অঙ্কন',
    'CSE-2022-011',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000005',
    'abir.ankon@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    9,
    'LIB-2026-0009',
    1,
    'Abul Kalam Azad',
    'আবুল কালাম আজাদ',
    'EEE-2022-012',
    'Electrical & Electronic Engineering',
    'B.Sc in EEE',
    'Spring 2026',
    '+8801711000006',
    'abul.kalam@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    10,
    'LIB-2026-0010',
    1,
    'Adil Ahmed Rahim',
    'আদিল আহমেদ রহিম',
    'CSE-2022-013',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000007',
    'adil.rahim@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    11,
    'LIB-2026-0011',
    1,
    'Ady Tye',
    'আদি তাই',
    'ENG-2022-014',
    'English',
    'B.A in English',
    'Spring 2026',
    '+8801711000008',
    'ady.tye@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    12,
    'LIB-2026-0012',
    1,
    'Ahona Islam',
    'অহনা ইসলাম',
    'BBA-2022-015',
    'Business Administration',
    'BBA',
    'Spring 2026',
    '+8801711000009',
    'ahona.islam@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    13,
    'LIB-2026-0013',
    1,
    'Aishi Anu',
    'ঐশী অনু',
    'CSE-2022-016',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000010',
    'aishi.anu@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    14,
    'LIB-2026-0014',
    1,
    'Akash Roy',
    'আকাশ রায়',
    'CSE-2022-017',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000011',
    'akash.roy@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    15,
    'LIB-2026-0015',
    1,
    'Al Amin Rifat',
    'আল আমিন রিফাত',
    'CSE-2022-018',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000012',
    'alamin.rifat@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    16,
    'LIB-2026-0016',
    1,
    'Amena Begum Akhi',
    'আমেনা বেগম আঁখি',
    'MAT-2022-019',
    'Mathematics',
    'B.Sc in Mathematics',
    'Spring 2026',
    '+8801711000013',
    'amena.akhi@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    17,
    'LIB-2026-0017',
    1,
    'Amrito Sarkar',
    'অমৃত সরকার',
    'CSE-2022-020',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000014',
    'amrito.sarkar@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    18,
    'LIB-2026-0018',
    1,
    'Farhana Hridi',
    'ফারহানা হৃদি',
    'CSE-2022-021',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000015',
    'farhana.hridi@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    19,
    'LIB-2026-0019',
    1,
    'Fahriya Riaz',
    'ফাহরিয়া রিয়াজ',
    'SWE-2022-022',
    'Software Engineering',
    'B.Sc in SWE',
    'Spring 2026',
    '+8801711000016',
    'fahriya.riaz@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    20,
    'LIB-2026-0020',
    1,
    'Hafsa Islam',
    'হাফসা ইসলাম',
    'BBA-2022-023',
    'Business Administration',
    'BBA',
    'Spring 2026',
    '+8801711000017',
    'hafsa.islam@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    21,
    'LIB-2026-0021',
    1,
    'Humyra Jahan Tanha',
    'হুমায়রা জাহান তানহা',
    'CSE-2022-024',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000018',
    'humyra.tanha@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    22,
    'LIB-2026-0022',
    1,
    'Israt Jahan',
    'ইসরাত জাহান',
    'CSE-2022-025',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000019',
    'israt.jahan@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    23,
    'LIB-2026-0023',
    1,
    'Mahfuja Momo',
    'মাহফুজা মোমো',
    'ECO-2022-026',
    'Economics',
    'B.S.S in Economics',
    'Spring 2026',
    '+8801711000020',
    'mahfuja.momo@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    24,
    'LIB-2026-0024',
    1,
    'Mahia Yasmin',
    'মাহিয়া ইয়াসমিন',
    'CSE-2022-027',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000021',
    'mahia.yasmin@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    25,
    'LIB-2026-0025',
    1,
    'Md Abdul Waddud Rihan',
    'মো. আব্দুল ওদ্দুদ রিহান',
    'CSE-2022-028',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000022',
    'waddud.rihan@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    26,
    'LIB-2026-0026',
    1,
    'Monirul Hasan Mithu',
    'মনিরুল হাসান মিঠু',
    'CSE-2022-029',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000023',
    'monirul.mithu@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    27,
    'LIB-2026-0027',
    1,
    'Najib Hasan Abir',
    'নাজিব হাসান আবির',
    'CSE-2022-030',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000024',
    'najib.abir@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    28,
    'LIB-2026-0028',
    1,
    'Nawsheen Tabassum',
    'নওশীন তাবাসসুম',
    'CSE-2022-031',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000025',
    'nawsheen.tabassum@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    29,
    'LIB-2026-0029',
    1,
    'Np Nayon Khan',
    'নয়ন খান',
    'CSE-2022-032',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000026',
    'nayon.khan@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    30,
    'LIB-2026-0030',
    1,
    'Nushrat Jahan',
    'নুসরাত জাহান',
    'BBA-2022-033',
    'Business Administration',
    'BBA',
    'Spring 2026',
    '+8801711000027',
    'nushrat.jahan@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    31,
    'LIB-2026-0031',
    1,
    'Nusrat Jahan',
    'নুসরত জাহান',
    'ENG-2022-034',
    'English',
    'B.A in English',
    'Spring 2026',
    '+8801711000028',
    'nusrat.jahan@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    32,
    'LIB-2026-0032',
    1,
    'Rafin Ahammed',
    'রাফিন আহমেদ',
    'CSE-2022-035',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000029',
    'rafin.ahammed@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    33,
    'LIB-2026-0033',
    1,
    'Rifah Salsabel Riha',
    'রিফাহ সালসাবিল রিহা',
    'CSE-2022-036',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000030',
    'rifah.riha@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    34,
    'LIB-2026-0034',
    1,
    'Samiha Sneha',
    'সামিহা স্নেহা',
    'CSE-2022-037',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000031',
    'samiha.sneha@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    35,
    'LIB-2026-0035',
    1,
    'Shad Islam Abir',
    'শাদ ইসলাম আবির',
    'CSE-2022-038',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000032',
    'shad.abir@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    36,
    'LIB-2026-0036',
    1,
    'Shihab Khan',
    'শিহাব খান',
    'CSE-2022-039',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000033',
    'shihab.khan@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    37,
    'LIB-2026-0037',
    1,
    'Solaiman Badsha',
    'সোলাইমান বাদশা',
    'CE-2022-040',
    'Civil Engineering',
    'B.Sc in CE',
    'Spring 2026',
    '+8801711000034',
    'solaiman.badsha@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    38,
    'LIB-2026-0038',
    1,
    'Mehdi Mahmud',
    'مهدي محمود',
    'IS-2022-041',
    'Islamic Studies',
    'B.A in Islamic Studies',
    'Spring 2026',
    '+8801711000035',
    'mehdi.mahmud@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    39,
    'LIB-2026-0039',
    1,
    'Yousuf Ali',
    'ইউসুফ আলী',
    'CSE-2022-042',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000036',
    'yousuf.ali@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    40,
    'LIB-2026-0040',
    1,
    'Zakia Nazmin',
    'জাকিয়া নাজমিন',
    'SWE-2022-043',
    'Software Engineering',
    'B.Sc in SWE',
    'Spring 2026',
    '+8801711000037',
    'zakia.nazmin@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    41,
    'LIB-2026-0041',
    1,
    'Prashanta Saha',
    'প্রশান্ত সাহা',
    'CSE-2022-044',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000038',
    'prashanta.saha@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    42,
    'LIB-2026-0042',
    1,
    'Srabonee Jahan Joita',
    'শ্রাবণী জাহান জয়ীতা',
    'CSE-2022-045',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000039',
    'srabonee.joita@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    43,
    'LIB-2026-0043',
    1,
    'Prosenjit Biswas',
    'প্রসেনজিৎ বিশ্বাস',
    'CSE-2022-046',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000040',
    'prosenjit.biswas@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    44,
    'LIB-2026-0044',
    1,
    'Emon Ahmed Simanto',
    'ইমন আহমেদ সীমান্ত',
    'CSE-2022-047',
    'Computer Science & Engineering',
    'B.Sc in CSE',
    'Spring 2026',
    '+8801711000041',
    'emon.simanto@libraryos.edu.bd',
    'Dhaka, Bangladesh',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    45,
    'LIB-2026-0045',
    5,
    'Rahman Sojibur',
    'রহমান সজীবুর',
    'SUP-2026-001',
    'External Patron / Supplier',
    'Vendor Affiliate',
    'N/A',
    '+8801811234502',
    'sojibur.rahman@dhakabooks.com',
    'Bangla Bazar, Dhaka',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    46,
    'LIB-2026-0046',
    5,
    'Sadia Afrin',
    'সাদিয়া আফরিন',
    'SUP-2026-002',
    'External Patron / Supplier',
    'Vendor Affiliate',
    'N/A',
    '+8801711234501',
    'sadia.afrin@booksupply.bd',
    'Nilkhet, Dhaka',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  ),
  (
    47,
    'LIB-2026-0047',
    5,
    'Sohan Ibn Sahid',
    'পুষ্প',
    'SUP-2026-003',
    'External Patron / Supplier',
    'Vendor Affiliate',
    'N/A',
    '+8801911234503',
    'pushpo.sahid@pushpobooks.com',
    'Mirpur-10, Dhaka',
    '2026-01-01',
    '2027-01-01',
    'ACTIVE'
  );

-- 3. Circulation Loans & Due Loans
-- Seed Loans 2 to 30: Kawsar Hoque (punctual), Sarah Rahman (punctual), Risalat Bepari (fined & overdue)
INSERT INTO
  loans (
    id,
    copy_id,
    member_id,
    issued_by,
    issued_at,
    due_date,
    returned_at,
    returned_to,
    return_condition,
    renewal_count,
    policy_fine_per_day_snapshot,
    status,
    notes
  )
VALUES
  -- Kawsar Hoque (Member 4): 3 Returned on-time + 2 Active on-time (zero late history)
  (
    2,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC057003'
    ),
    4,
    1,
    '2026-09-01 10:00:00',
    '2026-09-15',
    '2026-09-14 16:30:00',
    1,
    'EXCELLENT',
    0,
    5.00,
    'RETURNED',
    'Returned on time in pristine condition.'
  ),
  (
    3,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC200001'
    ),
    4,
    1,
    '2026-09-16 11:00:00',
    '2026-09-30',
    '2026-09-29 15:00:00',
    1,
    'GOOD',
    0,
    5.00,
    'RETURNED',
    'Returned on time.'
  ),
  (
    4,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC214001'
    ),
    4,
    1,
    '2026-10-01 09:00:00',
    '2026-10-15',
    '2026-10-14 17:00:00',
    1,
    'EXCELLENT',
    0,
    5.00,
    'RETURNED',
    'Returned on time.'
  ),
  (
    5,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC057004'
    ),
    4,
    1,
    '2026-11-01 10:00:00',
    '2026-11-15',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'Active loan: Clean Code. Punctual patron.'
  ),
  (
    6,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC059001'
    ),
    4,
    1,
    '2026-11-03 14:00:00',
    '2026-11-17',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'Active loan: The Pragmatic Programmer.'
  ),
  -- Sarah Rahman (Member 5): 2 Returned on-time + 2 Active on-time
  (
    7,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC041001'
    ),
    5,
    1,
    '2026-09-05 10:00:00',
    '2026-09-19',
    '2026-09-18 14:00:00',
    1,
    'EXCELLENT',
    0,
    5.00,
    'RETURNED',
    'Returned on time in flawless condition.'
  ),
  (
    8,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC075001'
    ),
    5,
    1,
    '2026-09-20 11:00:00',
    '2026-10-04',
    '2026-10-03 16:00:00',
    1,
    'GOOD',
    0,
    5.00,
    'RETURNED',
    'Returned on time.'
  ),
  (
    9,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC013001'
    ),
    5,
    1,
    '2026-11-02 11:00:00',
    '2026-11-16',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'Active loan: Himu Somogro. Star patron.'
  ),
  (
    10,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC180001'
    ),
    5,
    1,
    '2026-11-04 15:30:00',
    '2026-11-18',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'Active loan: SQL Antipatterns.'
  ),
  -- Risalat Bepari Rakiba (Member 6): 2 Overdue loans + 2 past late loans
  (
    11,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC062001'
    ),
    6,
    1,
    '2026-10-01 09:30:00',
    '2026-10-15',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'OVERDUE: 22 days overdue! Multiple notifications sent.'
  ),
  (
    12,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC197001'
    ),
    6,
    1,
    '2026-10-10 11:00:00',
    '2026-10-24',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'OVERDUE: 13 days overdue! Fine accruing daily at ৳5.00/day.'
  ),
  (
    13,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC016001'
    ),
    6,
    1,
    '2026-08-10 10:00:00',
    '2026-08-24',
    '2026-09-23 16:00:00',
    1,
    'FAIR',
    0,
    5.00,
    'RETURNED',
    'Returned 30 days late with worn edges.'
  ),
  (
    14,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC244001'
    ),
    6,
    1,
    '2026-09-01 10:00:00',
    '2026-09-15',
    '2026-10-05 14:00:00',
    1,
    'DAMAGED',
    0,
    5.00,
    'RETURNED',
    'Returned 20 days late with binding damage.'
  ),
  -- Other Friends: active loans & circulating items
  (
    15,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC001004'
    ),
    7,
    1,
    '2026-10-31 10:00:00',
    '2026-11-14',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'Active loan to Fariha Lubaba.'
  ),
  (
    16,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC002004'
    ),
    8,
    1,
    '2026-10-16 10:00:00',
    '2026-10-30',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'OVERDUE by 7 days (Abir Anjum Ankon).'
  ),
  (
    17,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC244002'
    ),
    10,
    1,
    '2026-11-02 10:00:00',
    '2026-11-16',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'Active loan to Adil Ahmed Rahim.'
  ),
  (
    18,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC021001'
    ),
    18,
    1,
    '2026-11-01 10:00:00',
    '2026-11-15',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'Active loan to Farhana Hridi.'
  ),
  (
    19,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC170001'
    ),
    27,
    1,
    '2026-10-14 15:30:00',
    '2026-10-28',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'OVERDUE by 9 days (Najib Hasan Abir).'
  ),
  (
    20,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC018001'
    ),
    42,
    1,
    '2026-11-03 10:00:00',
    '2026-11-17',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'Active loan to Srabonee Jahan Joita.'
  ),
  (
    21,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC008001'
    ),
    38,
    1,
    '2026-11-04 10:00:00',
    '2026-11-18',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'Active loan to Mehdi Mahmud.'
  ),
  (
    22,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC009001'
    ),
    39,
    1,
    '2026-10-18 12:00:00',
    '2026-11-01',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'OVERDUE by 5 days (Yousuf Ali).'
  ),
  (
    23,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC014001'
    ),
    34,
    1,
    '2026-11-01 10:00:00',
    '2026-11-15',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'Active loan to Samiha Sneha.'
  ),
  (
    24,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC015001'
    ),
    37,
    1,
    '2026-11-02 10:00:00',
    '2026-11-16',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'Active loan to Solaiman Badsha.'
  ),
  (
    25,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC063001'
    ),
    17,
    1,
    '2026-11-03 10:00:00',
    '2026-11-17',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'Active loan to Amrito Sarkar.'
  ),
  (
    26,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC270002'
    ),
    26,
    1,
    '2026-11-02 10:00:00',
    '2026-11-16',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'Active loan to Monirul Hasan Mithu.'
  ),
  (
    27,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC020001'
    ),
    21,
    1,
    '2026-11-04 10:00:00',
    '2026-11-18',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'Active loan to Humyra Jahan Tanha.'
  ),
  (
    28,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC214002'
    ),
    41,
    1,
    '2026-11-03 10:00:00',
    '2026-11-17',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'Active loan to Prashanta Saha.'
  ),
  (
    29,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC022001'
    ),
    23,
    1,
    '2026-11-01 10:00:00',
    '2026-11-15',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'Active loan to Mahfuja Momo.'
  ),
  (
    30,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC023001'
    ),
    35,
    1,
    '2026-11-02 10:00:00',
    '2026-11-16',
    NULL,
    NULL,
    NULL,
    0,
    5.00,
    'ACTIVE',
    'Active loan to Shad Islam Abir.'
  );

-- Update copy status to ISSUED for all active loans
UPDATE book_copies
SET
  status = 'ISSUED'
WHERE
  barcode IN (
    'BC057004',
    'BC059001',
    'BC013001',
    'BC180001',
    'BC062001',
    'BC197001',
    'BC001004',
    'BC002004',
    'BC244002',
    'BC021001',
    'BC170001',
    'BC018001',
    'BC008001',
    'BC009001',
    'BC014001',
    'BC015001',
    'BC063001',
    'BC270002',
    'BC020001',
    'BC214002',
    'BC022001',
    'BC023001'
  );

-- 4. Fines Table (Risalat heavily fined)
INSERT INTO
  fines (
    id,
    member_id,
    loan_id,
    fine_type,
    original_amount_bdt,
    waived_amount_bdt,
    paid_amount_bdt,
    status,
    created_by,
    created_at
  )
VALUES
  (
    1,
    6,
    11,
    'OVERDUE',
    110.00,
    0.00,
    0.00,
    'UNPAID',
    1,
    CURRENT_TIMESTAMP
  ),
  (
    2,
    6,
    12,
    'OVERDUE',
    65.00,
    0.00,
    0.00,
    'UNPAID',
    1,
    CURRENT_TIMESTAMP
  ),
  (
    3,
    6,
    13,
    'OVERDUE',
    150.00,
    20.00,
    50.00,
    'PARTIALLY_PAID',
    1,
    CURRENT_TIMESTAMP
  ),
  (
    4,
    6,
    14,
    'DAMAGE',
    250.00,
    50.00,
    0.00,
    'UNPAID',
    1,
    CURRENT_TIMESTAMP
  ),
  (
    5,
    8,
    16,
    'OVERDUE',
    35.00,
    0.00,
    0.00,
    'UNPAID',
    1,
    CURRENT_TIMESTAMP
  ),
  (
    6,
    27,
    19,
    'OVERDUE',
    45.00,
    0.00,
    0.00,
    'UNPAID',
    1,
    CURRENT_TIMESTAMP
  ),
  (
    7,
    39,
    22,
    'OVERDUE',
    25.00,
    0.00,
    0.00,
    'UNPAID',
    1,
    CURRENT_TIMESTAMP
  );

-- 5. Fine Adjustments
INSERT INTO
  fine_adjustments (
    id,
    fine_id,
    adjustment_type,
    amount_bdt,
    approved_by,
    reason,
    created_at
  )
VALUES
  (
    1,
    3,
    'WAIVER',
    20.00,
    1,
    'Academic exam period partial waiver approved by Principal',
    CURRENT_TIMESTAMP
  ),
  (
    2,
    4,
    'DISCOUNT',
    50.00,
    1,
    'Subsidized book re-binding cost approved by library committee',
    CURRENT_TIMESTAMP
  );

-- 6. Payments (Kawsar Hoque & Sarah highest payers)
INSERT INTO
  payments (
    id,
    receipt_number,
    member_id,
    amount_bdt,
    payment_method,
    external_reference,
    recorded_by,
    paid_at,
    is_reversed,
    notes
  )
VALUES
  (
    1,
    'REC-2026-0001',
    4,
    1200.00,
    'BKASH',
    'BK89012345',
    1,
    '2026-10-15 11:20:00',
    FALSE,
    'Annual Institutional Patron Membership Fee + Unlimited Digital Library Access'
  ),
  (
    2,
    'REC-2026-0002',
    4,
    800.00,
    'NAGAD',
    'NG22334455',
    1,
    '2026-10-25 15:45:00',
    FALSE,
    'Dedicated Study Desk & Inter-Library Delivery Service Subscription'
  ),
  (
    3,
    'REC-2026-0003',
    4,
    500.00,
    'BANK_TRANSFER',
    'TXN-77889900',
    1,
    '2026-11-02 14:10:00',
    FALSE,
    'LibraryOS Open Knowledge Contribution & Reader Support Fund'
  ),
  (
    4,
    'REC-2026-0004',
    5,
    1200.00,
    'BKASH',
    'BK66778899',
    1,
    '2026-10-16 10:15:00',
    FALSE,
    'Annual Institutional Patron Membership & Academic Digital Access'
  ),
  (
    5,
    'REC-2026-0005',
    5,
    650.00,
    'NAGAD',
    'NG44556677',
    1,
    '2026-10-28 16:30:00',
    FALSE,
    'Special Research Corner Subscription & E-Journal Access Pass'
  ),
  (
    6,
    'REC-2026-0006',
    6,
    50.00,
    'CASH',
    NULL,
    1,
    '2026-10-20 14:00:00',
    FALSE,
    'Partial settlement for Book 16 overdue fine'
  ),
  (
    7,
    'REC-2026-0007',
    7,
    300.00,
    'BKASH',
    'BK11223344',
    1,
    '2026-10-12 11:00:00',
    FALSE,
    'Annual Student Membership Fee (Fariha Lubaba)'
  ),
  (
    8,
    'REC-2026-0008',
    45,
    500.00,
    'CASH',
    NULL,
    1,
    '2026-10-18 15:00:00',
    FALSE,
    'Lifetime Reader Pass & Circulation Fee (Rahman Sojibur)'
  ),
  (
    9,
    'REC-2026-0009',
    18,
    300.00,
    'NAGAD',
    'NG55667788',
    1,
    '2026-10-19 12:30:00',
    FALSE,
    'Annual Student Membership Fee (Farhana Hridi)'
  ),
  (
    10,
    'REC-2026-0010',
    27,
    300.00,
    'BKASH',
    'BK99001122',
    1,
    '2026-10-22 14:00:00',
    FALSE,
    'Annual Student Membership Fee (Najib Hasan Abir)'
  ),
  (
    11,
    'REC-2026-0011',
    42,
    300.00,
    'BKASH',
    'BK33445566',
    1,
    '2026-10-24 16:15:00',
    FALSE,
    'Annual Student Membership Fee (Srabonee Jahan Joita)'
  ),
  (
    12,
    'REC-2026-0012',
    41,
    300.00,
    'NAGAD',
    'NG77889900',
    1,
    '2026-10-26 11:45:00',
    FALSE,
    'Annual Student Membership Fee (Prashanta Saha)'
  ),
  (
    13,
    'REC-2026-0013',
    38,
    300.00,
    'CASH',
    NULL,
    1,
    '2026-10-27 13:00:00',
    FALSE,
    'Annual Student Membership Fee (Mehdi Mahmud)'
  ),
  (
    14,
    'REC-2026-0014',
    10,
    300.00,
    'BKASH',
    'BK44556677',
    1,
    '2026-10-29 10:30:00',
    FALSE,
    'Annual Student Membership Fee (Adil Ahmed Rahim)'
  );

INSERT INTO
  payment_allocations (id, payment_id, fine_id, allocated_amount_bdt)
VALUES
  (1, 6, 3, 50.00);

-- 7. Reservations (Hold Queue)
INSERT INTO
  reservations (
    id,
    book_id,
    member_id,
    allocated_copy_id,
    status,
    requested_at,
    ready_at,
    expires_at,
    closed_at,
    notes
  )
VALUES
  (
    1,
    180,
    4,
    (
      SELECT
        id
      FROM
        book_copies
      WHERE
        barcode = 'BC180002'
    ),
    'READY_FOR_PICKUP',
    '2026-11-02 10:00:00',
    '2026-11-04 10:00:00',
    '2026-11-12 23:59:59',
    NULL,
    'Allocated on hold shelf for Kawsar Hoque.'
  ),
  (
    2,
    75,
    5,
    NULL,
    'WAITING',
    '2026-11-04 14:00:00',
    NULL,
    NULL,
    NULL,
    'Requested by Sarah Rahman.'
  ),
  (
    3,
    11,
    7,
    NULL,
    'WAITING',
    '2026-11-03 16:30:00',
    NULL,
    NULL,
    NULL,
    'Requested by Fariha Lubaba.'
  ),
  (
    4,
    8,
    45,
    NULL,
    'WAITING',
    '2026-11-02 11:00:00',
    NULL,
    NULL,
    NULL,
    'Requested by Rahman Sojibur.'
  ),
  (
    5,
    21,
    18,
    NULL,
    'WAITING',
    '2026-11-04 12:00:00',
    NULL,
    NULL,
    NULL,
    'Requested by Farhana Hridi.'
  ),
  (
    6,
    170,
    27,
    NULL,
    'WAITING',
    '2026-11-05 10:00:00',
    NULL,
    NULL,
    NULL,
    'Requested by Najib Hasan Abir.'
  ),
  (
    7,
    18,
    42,
    NULL,
    'WAITING',
    '2026-11-03 14:00:00',
    NULL,
    NULL,
    NULL,
    'Requested by Srabonee Jahan Joita.'
  ),
  (
    8,
    63,
    41,
    NULL,
    'WAITING',
    '2026-11-04 15:00:00',
    NULL,
    NULL,
    NULL,
    'Requested by Prashanta Saha.'
  ),
  (
    9,
    57,
    26,
    NULL,
    'WAITING',
    '2026-11-05 09:30:00',
    NULL,
    NULL,
    NULL,
    'Requested by Monirul Hasan Mithu.'
  ),
  (
    10,
    13,
    21,
    NULL,
    'WAITING',
    '2026-11-03 17:00:00',
    NULL,
    NULL,
    NULL,
    'Requested by Humyra Jahan Tanha.'
  ),
  (
    11,
    41,
    23,
    NULL,
    'WAITING',
    '2026-11-04 11:30:00',
    NULL,
    NULL,
    NULL,
    'Requested by Mahfuja Momo.'
  ),
  (
    12,
    59,
    35,
    NULL,
    'WAITING',
    '2026-11-05 14:00:00',
    NULL,
    NULL,
    NULL,
    'Requested by Shad Islam Abir.'
  );

-- Update allocated reservation copy to RESERVED
UPDATE book_copies
SET
  status = 'RESERVED'
WHERE
  barcode = 'BC180002';

-- 8. Acquisitions & Procurement (Sadia, Sojibur, Pushpo)
INSERT INTO
  acquisitions (
    id,
    acquisition_number,
    supplier_id,
    invoice_number,
    order_date,
    received_date,
    total_cost_bdt,
    status,
    notes,
    created_by,
    created_at
  )
VALUES
  (
    1,
    'PO-2026-0001',
    4,
    'INV-SADIA-2026-88',
    '2026-10-10',
    '2026-10-22',
    48500.00,
    'RECEIVED',
    'Procurement of Academic Software Engineering & Algorithms collections from Sadia Book Agency.',
    1,
    CURRENT_TIMESTAMP
  ),
  (
    2,
    'PO-2026-0002',
    5,
    'INV-SOJIBUR-2026-104',
    '2026-10-20',
    '2026-10-30',
    72000.00,
    'RECEIVED',
    'Bulk procurement of Bengali Literature, Historical works, and Islamic references from Rahman Sojibur Book Distributors.',
    1,
    CURRENT_TIMESTAMP
  ),
  (
    3,
    'PO-2026-0003',
    6,
    'INV-PUSHPO-2026-302',
    '2026-11-01',
    NULL,
    35600.00,
    'ORDERED',
    'Procurement of World Literature and Personal Development bestsellers placed with Pushpo Academic Supply.',
    1,
    CURRENT_TIMESTAMP
  );

INSERT INTO
  acquisition_items (
    acquisition_id,
    book_id,
    quantity,
    unit_cost_bdt,
    received_quantity
  )
VALUES
  (1, 57, 20, 1100.00, 20),
  (1, 59, 20, 750.00, 20),
  (1, 62, 10, 1150.00, 10),
  (2, 1, 45, 200.00, 45),
  (2, 13, 25, 650.00, 25),
  (2, 18, 30, 280.00, 30),
  (2, 21, 35, 200.00, 35),
  (2, 8, 15, 320.00, 15),
  (3, 41, 25, 600.00, 0),
  (3, 75, 20, 700.00, 0),
  (3, 80, 12, 550.00, 0);

-- 9. Reviews & Ratings
INSERT INTO
  reviews (
    book_id,
    member_id,
    rating,
    review_title,
    review_text,
    is_approved,
    is_reported,
    created_at
  )
VALUES
  (
    57,
    4,
    5,
    'Essential Masterpiece for Every Software Engineer',
    'Writing clean, readable code is a discipline. Uncle Bob''s guidelines on functions, naming, and refactoring are invaluable for any professional engineer.',
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP
  ),
  (
    59,
    4,
    5,
    'Pragmatic Wisdom for Modern Developers',
    'A timeless guide on software craftsmanship, orthodox thinking, and continuous learning.',
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP
  ),
  (
    41,
    5,
    5,
    'Life Changing Book on Habit Formation',
    'Small 1% daily improvements compound into remarkable results. Clear, actionable, and inspiring.',
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP
  ),
  (
    75,
    5,
    5,
    'A Breathtaking Journey Through Human History',
    'Yuval Noah Harari''s perspective on cognitive revolution, myths, and agriculture is deeply eye-opening.',
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP
  ),
  (
    62,
    6,
    4,
    'Solid Architecture Reference',
    'Classic Gang of Four patterns. Takes significant effort to internalize each creational, structural, and behavioral pattern, but very rewarding.',
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP
  ),
  (
    8,
    45,
    5,
    'Authentic & Enlightening',
    'Dr. Abdullah Jahangir''s authoritative work on Sunnah-based Zikr and daily lifestyle.',
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP
  ),
  (
    197,
    27,
    5,
    'Best Operating Systems Book Ever',
    'Virtualization, concurrency, and persistence explained with unmatched clarity and humor.',
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP
  ),
  (
    21,
    18,
    5,
    'Soulful Masterpiece',
    'The nostalgia and rural beauty portrayed in this book touch the deepest core of the heart.',
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP
  ),
  (
    63,
    41,
    5,
    'Timeless Project Management Lessons',
    'Adding manpower to a late software project makes it later. Essential reading for all tech leads.',
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP
  );