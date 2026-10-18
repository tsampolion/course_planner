# EduSuite Home, Settings, Overdue and Privacy: design

Agreed with the owner on 2026-10-18. The implementation plan is `docs/superpowers/plans/2026-10-18-edusuite-home-settings-privacy.md`.

## Design (agreed decisions)

- **A. Footer.** Remove *part of EduSuite apps* from Home in both apps. It appears only in Settings → About. Pacer already has `settings_suite`; Invoicer gains it.
- **B. Grid.**
  - `columns = floor((width + gap) / (160dp + gap))`, between 2 and the tile count. It drops to 1 at font scale ≥ 2 or width < 360dp.
  - No max-width cap on the grid. Pacer's hero stays capped at 560dp.
  - Tiles glide to new positions (`Motion.settle`) when the count changes, and snap when animations are off.
  - Rotation no longer recreates the Activity (`configChanges`), so the glide plays.
  - Every tile stays composed: no lazy grid, because Invoicer's `EndToEndTest` uses `performScrollTo`.
- **C. Settings (Invoicer).**
  - Statement fields (name, address, show address, phone, email, IBAN, footer) move to a **Statement details** screen. It opens from a row at the top of Statements.
  - Create statement shows a bottom sheet asking for name and IBAN when the name is blank.
  - Settings becomes: Appearance, Lessons (default length), Reminders, Privacy, Backup, About.
- **D. Overdue (Invoicer).**
  - A statement is overdue when it is not void, not paid, and today is after the last day of its issue month.
  - New statements carry the note `Payment due by <last day of issue month>`, frozen in the document. Older ones print as they were.
  - Overdue figures use the suite red (`Invoicer.colors.warn`, mapped to `colorScheme.error`).
  - Home: the Statements tile subtitle reads `N overdue` in red with an `Overdue` warning chip, and a warning banner above the grid says `N statements overdue · View`.
  - Students: rows with an overdue statement show an `Overdue` warning chip, and a banner says `N students have overdue statements`.
- **E. Privacy (Invoicer, GDPR = Regulation (EU) 2016/679).** The teacher is the controller (Art. 4(7)). The app helps them comply and collects no more than it needs.
  1. **In-app notices.**
     - A hint on the student form: "Only what you need for lessons and billing. It stays on this device. Don't put health or other sensitive details in notes." Optional fields are labelled "(optional)".
     - A line on Statement details: "You are responsible for the personal data you keep here (GDPR). Share a privacy notice with your students."
     - A Settings → Privacy section with the same text, *Share privacy notice*, and a warning: "Backups are not encrypted: keep them somewhere only you can open."
  2. **Notice for parents.** An Art. 13 notice in English and Greek, filled from Statement details and shared as text through the share sheet. The fixed copy is below.
  3. **Minimise and erase.** *Erase personal details* on a student clears class, guardian, phone, email and notes, archives them, and keeps the name only on lessons and statements already issued (Art. 17(3)(b)). *Delete* stays for students with no history (R6 unchanged).
  4. **Export a student's data.** A JSON file with the student, their lessons and their statements, shared through the existing `FileProvider` (Art. 15, 20).

### Fixed copy: parent notice

**English**:

```
Privacy notice: lessons with {name}

Who is responsible
{name}{, address}{, phone}{, email} is the controller of the personal data below.

What I keep
The student's name and class, the guardian's name, the phone number and email you give me, the dates, length and fees of lessons, payments, and the statements I send you.

Why, and on what legal basis
To arrange and bill lessons, which is necessary for our agreement (GDPR Art. 6(1)(b)), and to keep the billing records the law requires (Art. 6(1)(c)).

Who receives it
No one else. The data stays on my own device. It is not sent to any online service or outside the EU. I may show billing records to my accountant or the tax authority when the law requires it.

How long
While lessons continue, then for as long as Greek tax law requires me to keep billing records (generally five years from the end of the tax year). Contact details are erased when lessons end.

Your rights
You may ask me for access to your data, its rectification or erasure, restriction of processing, to object to processing, and for a copy in a portable format (Art. 15–21), at the contact details above.

Complaints
You may lodge a complaint with the Hellenic Data Protection Authority, 1-3 Kifisias Ave., 115 23 Athens, tel. +30 210 6475600, www.dpa.gr.

Names and lesson details are needed to arrange and bill lessons; phone, email and the guardian's name are optional. No decisions are made automatically and there is no profiling.
```

**Ελληνικά** (with the terms of the official Greek text of the GDPR):

```
Ενημέρωση για την προστασία δεδομένων: μαθήματα με {name}

Υπεύθυνος επεξεργασίας
{name}{, address}{, phone}{, email} είναι ο υπεύθυνος επεξεργασίας των παρακάτω δεδομένων προσωπικού χαρακτήρα.

Ποια δεδομένα τηρώ
Το ονοματεπώνυμο και την τάξη του μαθητή, το όνομα του κηδεμόνα, το τηλέφωνο και το email που μου δίνετε, τις ημερομηνίες, τη διάρκεια και την αμοιβή των μαθημάτων, τις πληρωμές και τις καταστάσεις που σας στέλνω.

Σκοπός και νομική βάση
Η οργάνωση και η χρέωση των μαθημάτων, που είναι απαραίτητη για την εκτέλεση της συμφωνίας μας (άρθρο 6 παρ. 1 στοιχ. β ΓΚΠΔ), και η τήρηση των στοιχείων χρέωσης που επιβάλλει ο νόμος (άρθρο 6 παρ. 1 στοιχ. γ).

Αποδέκτες
Κανείς άλλος. Τα δεδομένα βρίσκονται μόνο στη δική μου συσκευή. Δεν αποστέλλονται σε διαδικτυακή υπηρεσία ούτε εκτός ΕΕ. Μπορεί να τα δει ο λογιστής μου ή η φορολογική αρχή, όταν το απαιτεί ο νόμος.

Χρονικό διάστημα αποθήκευσης
Όσο συνεχίζονται τα μαθήματα και στη συνέχεια για όσο διάστημα η ελληνική φορολογική νομοθεσία απαιτεί να τηρώ στοιχεία χρέωσης (κατά κανόνα πέντε έτη από τη λήξη του φορολογικού έτους). Τα στοιχεία επικοινωνίας διαγράφονται όταν τελειώσουν τα μαθήματα.

Τα δικαιώματά σας
Έχετε δικαίωμα πρόσβασης στα δεδομένα σας, διόρθωσης ή διαγραφής τους, περιορισμού της επεξεργασίας, εναντίωσης στην επεξεργασία και φορητότητας των δεδομένων (άρθρα 15–21), στα παραπάνω στοιχεία επικοινωνίας.

Καταγγελία
Έχετε δικαίωμα υποβολής καταγγελίας στην Αρχή Προστασίας Δεδομένων Προσωπικού Χαρακτήρα, Λεωφ. Κηφισίας 1-3, 115 23 Αθήνα, τηλ. 210 6475600, www.dpa.gr.

Το ονοματεπώνυμο και τα στοιχεία των μαθημάτων χρειάζονται για την οργάνωση και τη χρέωση των μαθημάτων· το τηλέφωνο, το email και το όνομα του κηδεμόνα είναι προαιρετικά. Δεν λαμβάνονται αυτοματοποιημένες αποφάσεις και δεν γίνεται κατάρτιση προφίλ.
```

**Sources:**
- GDPR EN/EL official texts: [EUR-Lex EN](https://eur-lex.europa.eu/legal-content/EN/TXT/HTML/?uri=CELEX:32016R0679), [EUR-Lex EL](https://eur-lex.europa.eu/legal-content/EL/TXT/HTML/?uri=CELEX:32016R0679). The Art. 4(7), 6(1), 13, 15–22 wording was checked against both.
- HDPA address and phone: [dpa.gr](https://www.dpa.gr/en/personal_data/controller_details_contact).
- Five-year retention (Law 4174/2013 art. 13(2), Law 4308/2014): [taxlaw.gr](https://www.taxlaw.gr/en/special-topics/personal-data-retention-rules-currently-applicable-in-greece-2/), [e-forologia](https://www.e-forologia.gr/cms/viewContents.aspx?id=211361). The notice says "generally five years" because the tax-procedure code has since been recodified.
- Invoicer statements are non-fiscal (R14).

