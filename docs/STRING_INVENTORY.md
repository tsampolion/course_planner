# String inventory

Every word Course Pacer shows, says aloud (TalkBack) or prints, so the owner can propose new wording in one place. **No wording has been changed.** Fill the *Your proposal* column and return the file; copy changes then happen in one pass.

- **Resource strings:** 791 entries from `app/src/main/res/values/*strings.xml`, grouped by screen. Plurals are one row each, with their `one` and `other` forms. `%1$s`, `%d` and similar are filled in by the app (names, numbers, dates); keep them in any proposal.
- **TalkBack labels** are resource strings too (content descriptions, state descriptions and custom actions). Rows whose ID looks like one are tagged *TalkBack* in Notes; others used only by TalkBack are not separately tagged, so treat every row as possibly spoken.
- **Strings written in Kotlin** (last section) are engine messages, default names, printed output and exports. They are kept out of resources on purpose, so changing them is a code change; say which you want changed.

## App bar, menu and shared actions

44 entries.

| ID | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `app_name` | Course Pacer |  |  |
| `menu_help` | Help |  |  |
| `menu_lessons` | Lessons |  |  |
| `menu_share_print` | Share & print |  |  |
| `app_settings` | Settings |  |  |
| `app_year_settings` | School year settings |  |  |
| `app_overview` | Pacing overview |  |  |
| `app_classes` | Classes |  |  |
| `app_class` | Class |  |  |
| `app_group` | Group |  |  |
| `app_undo` | Undo |  |  |
| `app_deleted` | Deleted “%1$s” |  | Has placeholders |
| `app_copied` | Copied. Paste into a spreadsheet or Word. |  |  |
| `app_schedule` | Schedule |  |  |
| `app_undo_steps` | one: Undo (%1$d) / other: Undo (%1$d) |  | Plural, Has placeholders |
| `nav_menu` | Menu |  |  |
| `nav_search` | Find a class or group |  |  |
| `nav_clear_search` | Clear search |  |  |
| `nav_no_match` | No class or group matches “%1$s”. |  | Has placeholders |
| `nav_home` | Home |  |  |
| `nav_year` | School year |  |  |
| `nav_year_named` | School year · %1$s |  | Has placeholders |
| `nav_show_classes` | Show classes and groups |  |  |
| `nav_hide_classes` | Hide classes and groups |  |  |
| `nav_classes_groups` | Classes & Groups |  |  |
| `nav_back_to` | Back to %1$s |  | Has placeholders |
| `nav_heading_count` | %1$s · %2$s |  | Has placeholders: a class name and its group count |
| `nav_current_page` | Current page |  |  |
| `nav_pin` | Keep the menu open |  |  |
| `nav_unpin` | Let the menu collapse |  |  |
| `group_place_home` | Group home |  |  |
| `bar_more_warnings` | %1$s, %2$s |  | Has placeholders |
| `nav_class_group` | %1$s · %2$s |  | Has placeholders |
| `nav_behind` | one: %d behind / other: %d behind |  | Plural, Has placeholders |
| `nav_groups_behind` | one: %d group behind / other: %d groups behind |  | Plural, Has placeholders |
| `nav_all_classes` | one: The class / other: All %d classes |  | Plural, Has placeholders |
| `switcher_title` | Go to |  |  |
| `switcher_open` | Switch group |  |  |
| `status_on_pace` | On pace |  |  |
| `status_short` | one: Short by %d lesson / other: Short by %d lessons |  | Plural, Has placeholders |
| `rows_options` | Options for %1$s |  | Has placeholders |
| `rows_delete` | Delete |  |  |
| `panes_pick_group` | Pick a group to open it here. |  |  |
| `toast_nothing_changed` | Already up to date |  |  |
| `save_not_saved` | Couldn't save your latest changes. They are still in the app; keep it open until they are saved. |  |  |
| `save_read_locked` | Couldn't read your saved plans right now. Nothing was changed, and changes made now aren't being saved. |  |  |
| `save_read_locked_edits` | Your saved plans can be read again, but the changes made since would replace them, so nothing is being saved. Export a copy, then close and reopen the app. |  |  |
| `save_retry` | Retry |  |  |
| `save_read_again` | Try reading again |  |  |
| `save_export` | Export a copy |  |  |
| `save_exported` | Saved a copy of your changes |  |  |
| `nav_menu_update` | Menu, update available |  |  |
| `nav_update` | Update |  |  |
| `nav_update_description` | Update available |  | TalkBack |

## Home

53 entries.

| ID | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `home_create_year` | Create your school year |  |  |
| `home_create_year_hint` | Give it its first and last teaching day. The Greek public holidays can come in as days off. |  |  |
| `home_new_year` | New school year |  |  |
| `home_year` | School year |  |  |
| `home_hello` | Hello |  |  |
| `home_hello_name` | Hello, %1$s |  | Has placeholders |
| `home_next_today` | Next · today · %1$d of %2$d left |  | Has placeholders |
| `home_next_on` | Next · %1$s |  | Has placeholders |
| `home_next_overdue` | Next · overdue · %1$s |  | Has placeholders |
| `home_done` | Done |  |  |
| `home_all_classes` | All %1$d › |  | Has placeholders |
| `home_see_all` | See all › |  |  |
| `home_board_empty` | Add a group to a class and it shows here. |  |  |
| `home_legend_behind` | Behind, lessons short |  |  |
| `home_cell_behind` | %1$s −%2$d |  | Has placeholders |
| `home_cell` | %1$s %2$s |  | Has placeholders |
| `home_cell_on_pace` | %1$s %2$s, on pace |  | Has placeholders |
| `home_open_group` | Open group |  |  |
| `home_add_first_class` | Add your first class |  |  |
| `home_first_class_hint` | A class holds its books, lesson days and groups. Add one and its next lesson shows here. |  |  |
| `home_add_class` | Add a class |  |  |
| `home_no_lessons` | No lessons left this term |  |  |
| `home_overview_hint` | Open Pacing overview to see where each group stands. |  |  |
| `home_lesson_done` | Lesson marked done |  |  |
| `home_classes` | Classes |  |  |
| `home_overview` | Pacing overview |  |  |
| `home_dates_missing` | Dates not set |  |  |
| `home_rename` | Rename |  |  |
| `home_class` | Class |  |  |
| `home_group` | Group |  |  |
| `home_no_pages` | No pages |  |  |
| `home_rename_year` | Rename school year |  |  |
| `home_save` | Save |  |  |
| `home_name` | Name |  |  |
| `home_create` | Create |  |  |
| `home_starts` | Starts |  |  |
| `home_ends` | Ends |  |  |
| `home_holidays` | Add the Greek public holidays as days off |  |  |
| `home_date_range` | %1$s – %2$s |  | Has placeholders |
| `home_pair` | %1$s · %2$s |  | Has placeholders |
| `home_classes_count` | one: %d class / other: %d classes |  | Plural, Has placeholders |
| `home_groups_count` | one: %d group / other: %d groups |  | Plural, Has placeholders |
| `home_behind_count` | one: %d behind / other: %d behind |  | Plural, Has placeholders |
| `home_cell_short` | one: %1$s %2$s, short by %3$d lesson / other: %1$s %2$s, short by %3$d lessons |  | Plural, Has placeholders |
| `today_label` | Today · lesson %1$d |  | Has placeholders |
| `today_next_label` | Next · lesson %1$d |  | Has placeholders |
| `today_done` | Done |  |  |
| `today_pages_taught` | Pages taught |  |  |
| `hint_got_it` | Got it |  |  |
| `hint_lesson_menu` | Long-press a lesson, or use TalkBack's actions, to change its pages, mark everything up to it done, or plan it. |  |  |
| `hint_pace_track` | The tick marks where a book would be in step with the lead book. Tap a book to change where the group is in it. |  |  |
| `home_classes_link` | Classes › |  |  |
| `home_groups_on_pace` | one: %d group on pace › / other: %d groups on pace › |  | Plural, Has placeholders |

## Welcome guide and tips

19 entries.

| ID | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `guide_step_1_title` | School year |  |  |
| `guide_step_1_detail` | Its first and last teaching day, and the days off. |  |  |
| `guide_step_2_title` | Class |  |  |
| `guide_step_2_detail` | The lesson length and the pace you can sustain. |  |  |
| `guide_step_3_title` | Books and groups |  |  |
| `guide_step_3_detail` | Each book's first and last page, and each group's lesson days. |  |  |
| `guide_step_4_title` | Schedule |  |  |
| `guide_step_4_detail` | Open a group to see the pages for every lesson, and tick lessons as you teach them. |  |  |
| `guide_welcome` | Welcome to %1$s |  | Has placeholders |
| `guide_intro` | %1$s spreads your coursebook pages over the lessons left in the term, so you can see whether each group will finish in time. |  | Has placeholders |
| `guide_four_steps` | Four steps to your first schedule: |  |  |
| `guide_hint` | Each screen says what to do next while it's empty. You can open this guide again from Settings. |  |  |
| `guide_create_year` | Create your school year |  |  |
| `guide_got_it` | Got it |  |  |
| `guide_not_now` | Not now |  |  |
| `guide_next_step` | Next step |  |  |
| `guide_dismiss` | Dismiss |  |  |
| `guide_step_number` | %1$d |  | Has placeholders |
| `guide_schedule_note` | The schedule is arithmetic, not advice: it spreads each book's pages evenly over the lessons left. It can't know how your class is really doing, so go by what you actually teach and record it here as you go. |  |  |

## Classes and class editor

115 entries.

| ID | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `class_positions_reset` | Positions reset |  |  |
| `class_deleted` | Deleted “%1$s” |  | Has placeholders |
| `class_removed` | Removed “%1$s” |  | Has placeholders |
| `class_class` | Class |  |  |
| `class_group` | Group |  |  |
| `class_book` | Book |  |  |
| `class_test` | Test |  |  |
| `class_takes_lesson` | takes the lesson |  |  |
| `class_alongside_lesson` | alongside the lesson |  |  |
| `class_page_range` | pp. %1$d–%2$d |  | Has placeholders |
| `class_due_by` | by %1$s |  | Has placeholders |
| `class_book_summary` | %1$s · %2$s |  | Has placeholders |
| `class_homework_pages` | %1$s %2$d–%3$d |  | Has placeholders |
| `class_set_on` | Set %1$s |  | Has placeholders |
| `class_no_homework_group` | None yet |  |  |
| `class_problems_title` | Problems in this class |  |  |
| `class_book_guide` | Add each coursebook with its first and last page, like Student's Book pp. 1–160. Each book is paced on its own. Then check each group's lesson days below and tap Open schedule. |  |  |
| `class_add_the_books_you_teach_from` | Add the books you teach from |  |  |
| `class_add_a_book` | Add a book |  |  |
| `class_add_a_group` | Add a group |  |  |
| `class_a_group_is_a_set_of_students_taught_from_these_books_with_it` | A group is a set of students taught from these books, with its own lesson days and place in the books. |  |  |
| `class_add_group` | Add group |  |  |
| `class_name` | Name |  |  |
| `class_books` | Books |  |  |
| `class_add` | Add |  |  |
| `class_each_book_is_paced_on_its_own_skipped_pages_apply_to_every_g` | Each book is paced on its own. Skipped pages apply to every group; homework applies to one group. |  |  |
| `class_no_books_yet` | No books yet. |  |  |
| `class_groups` | Groups |  |  |
| `class_add_a_group_for_each_set_of_students_taught_from_these_books` | Add a group for each set of students taught from these books. |  |  |
| `class_tests_and_homework` | Tests and homework |  |  |
| `class_tests_worksheets` | Tests & worksheets |  |  |
| `class_homework_already_assigned` | Homework already assigned |  |  |
| `class_reset_all_positions` | Reset all positions |  |  |
| `class_delete_this_class` | Delete this class |  |  |
| `class_class_settings` | Class settings |  |  |
| `class_done` | Done |  |  |
| `class_lesson_length_min` | Lesson length (min) |  |  |
| `class_pages_per_lesson_is_the_benchmark_the_verdict_measures_again` | Pages per lesson is the benchmark the verdict measures against; it does not drive the schedule. |  |  |
| `class_edit` | Edit |  |  |
| `class_next_lesson` | Next lesson |  |  |
| `class_remove_group` | Remove group |  |  |
| `class_open_schedule` | Open schedule |  |  |
| `class_no_tests_yet` | No tests yet |  |  |
| `class_plan_a_test_or_worksheet_for_your_next_lesson` | Plan a test or worksheet for your next lesson. |  |  |
| `class_add_a_test` | Add a test |  |  |
| `class_upcoming` | Upcoming |  |  |
| `class_no_upcoming_tests` | No upcoming tests. |  |  |
| `class_all_groups` | All groups |  |  |
| `class_not_a_lesson_day` | Not a lesson day |  |  |
| `class_title` | Title |  |  |
| `class_first_page` | First page |  |  |
| `class_last_page` | Last page |  |  |
| `class_skip_pages` | Skip pages |  |  |
| `class_8_24_27` | 8, 24–27 |  |  |
| `class_finish_by` | Finish by |  |  |
| `class_only_this_book_re_paces_the_others_keep_their_own_rate` | Only this book re-paces. The others keep their own rate. |  |  |
| `class_remove_book` | Remove book |  |  |
| `class_pages_set_for_homework_leave_the_classroom_sequence_the_rest` | Pages set for homework leave the classroom sequence. The rest re-spread, so the pace eases and the books still finish together. |  |  |
| `class_no_homework_assigned` | No homework assigned |  |  |
| `class_assign_pages_to_ease_the_classroom_pace` | Assign pages to ease the classroom pace. |  |  |
| `class_add_homework` | Add homework |  |  |
| `class_add_a_group_and_a_book_first` | Add a group and a book first. |  |  |
| `class_add_a_test_or_worksheet` | Add a test or worksheet |  |  |
| `class_unit_3_test` | Unit 3 test |  |  |
| `class_date` | Date |  |  |
| `class_takes_the_whole_lesson` | Takes the whole lesson |  |  |
| `class_companion` | Companion |  |  |
| `class_from_p` | From p. |  |  |
| `class_to_p` | To p. |  |  |
| `class_set_on_optional` | Set on (optional) |  |  |
| `class_entries` | one: %d entry / other: %d entries |  | Plural, Has placeholders |
| `class_assignments` | one: %d assignment / other: %d assignments |  | Plural, Has placeholders |
| `class_lessons_left` | one: %d lesson left / other: %d lessons left |  | Plural, Has placeholders |
| `class_past_tests` | one: Past test (%d) / other: Past tests (%d) |  | Plural, Has placeholders |
| `class_teachable` | one: %d teachable page / other: %d teachable pages |  | Plural, Has placeholders |
| `class_pages` | one: %d page / other: %d pages |  | Plural, Has placeholders |
| `class_monday` | Mon |  |  |
| `class_tuesday` | Tue |  |  |
| `class_wednesday` | Wed |  |  |
| `class_thursday` | Thu |  |  |
| `class_friday` | Fri |  |  |
| `class_saturday` | Sat |  |  |
| `class_sunday` | Sun |  |  |
| `class_lead_book` | Lead book |  |  |
| `class_lead_none` | None: each book keeps its own pace |  |  |
| `class_lead_hint` | The other books keep step with the lead book: when it is a third of the way through, so are they. A book with its own finish-by date keeps its own pace. Give the books unit start pages to keep them in step unit by unit. |  |  |
| `class_units` | Unit start pages |  |  |
| `class_units_placeholder` | 1, 13, 25, 37 |  |  |
| `class_units_hint` | The page each unit starts on. When this book and the lead book have the same number of units, this book keeps step unit by unit. |  |  |
| `class_units` | one: %d unit / other: %d units |  | Plural, Has placeholders |
| `class_lag_lessons` | Warn when a book is this many lessons behind |  |  |
| `class_lag_hint` | The group screen warns when one book falls this far behind the lead book (or the others), and offers to bring it back in step. |  |  |
| `classes_behind` | one: %d group behind / other: %d groups behind |  | Plural, Has placeholders |
| `class_summary` | %1$d min lessons · %2$s pages a lesson |  | Has placeholders |
| `class_upcoming_next` | %1$s · next %2$s |  | Has placeholders |
| `class_upcoming_count` | one: %d upcoming / other: %d upcoming |  | Plural, Has placeholders |
| `class_next_on` | Next %1$s |  | Has placeholders |
| `class_edit_group` | Edit %1$s |  | Has placeholders |
| `class_pages_chosen` | one: %d page chosen / other: %d pages chosen |  | Plural, Has placeholders |
| `class_skips` | skips %1$s |  | Has placeholders |
| `class_show` | Show |  |  |
| `class_hide` | Hide |  |  |
| `collections_sort` | Classes: %1$s |  | Has placeholders |
| `collections_your_order` | Your order |  |  |
| `collections_name_order` | Name |  |  |
| `collections_behind_order` | Most behind |  |  |
| `collections_new` | New collection… |  |  |
| `collections_edit` | Edit collection |  |  |
| `collections_uncollected` | Other classes |  |  |
| `collections_move_hint` | Choose classes. A class selected from another collection moves here. |  |  |
| `collections_saved` | Collection saved |  |  |
| `collections_delete` | Delete collection |  |  |
| `collections_deleted` | Deleted collection “%1$s” |  | Has placeholders |
| `collections_keep_classes` | Deleting this collection keeps all its classes. |  |  |
| `collections_empty` | No classes in this collection. |  |  |

## Group (home, settings, menu)

88 entries.

| ID | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `group_updated` | Lesson updated |  |  |
| `group_marked_done` | Lessons marked done |  |  |
| `group_marked_not_done` | Lessons marked not done |  |  |
| `group_saved` | Pages taught saved |  |  |
| `group_lesson_done` | Lesson marked done |  |  |
| `group_copy` | Copy |  |  |
| `group_share` | Share |  |  |
| `group_add_books` | Add this class's books |  |  |
| `group_add_books_hint` | Each lesson shows its pages once the class has books. Add them on the class screen. |  |  |
| `group_no_range` | No lessons left in this range. |  |  |
| `group_edit_pages_menu` | Record what I taught… |  |  |
| `group_done_up_to` | Done up to here |  |  |
| `group_not_done_from` | Not done from here |  |  |
| `group_edited` | Edited |  |  |
| `group_done` | Done |  |  |
| `group_save` | Save |  |  |
| `group_none` | none |  |  |
| `group_mark_done` | Mark done |  |  |
| `group_lessons_left` | Lessons left |  |  |
| `group_pages_left` | Pages left |  |  |
| `group_pace` | Required pace |  |  |
| `group_peak` | Heaviest lesson |  |  |
| `group_hours` | Contact hours |  |  |
| `group_banked` | Pages banked as homework |  |  |
| `group_due_passed` | Due passed |  |  |
| `group_position_hint` | The next page to teach. Updating it redistributes the remaining pages over future lessons. |  |  |
| `group_resume` | Resume at page |  |  |
| `group_schedule` | %1$s · schedule |  | Has placeholders |
| `group_tick_hint` | Tick a lesson once it's taught. Long-press a lesson to change the pages it covered, to mark everything up to it, or to undo from it. |  |  |
| `group_book_pages` | %1$s: %2$s |  | Has placeholders |
| `ledger_retired_pages` | %1$s %2$s (not in plan) |  | Has placeholders |
| `ledger_retired_spoken` | %1$s: %2$s, no longer in the group's plan |  | Has placeholders |
| `group_book_number` | Book %1$d |  | Has placeholders |
| `group_pages_hint` | Pages taught from each book, like 5-8, 10. Leave a book empty if you taught none. Pages left out go back into the schedule, and extra pages come off later lessons.%1$s |  | Has placeholders |
| `group_saving_marks_done` | " Saving marks the lesson done." |  |  |
| `group_page_error` | Use pages %1$d–%2$d, like 5-8, 10 |  | Has placeholders |
| `group_planned` | Planned: %1$s |  | Has placeholders |
| `group_due` | By %1$s |  | Has placeholders |
| `group_position` | %1$s · position |  | Has placeholders |
| `group_no_term_lessons` | No lessons left in this term |  |  |
| `group_pace_needed` | Pace needed |  |  |
| `group_blocked_lessons` | Blocked lessons |  |  |
| `group_no_before_due` | No lesson before due |  |  |
| `group_no_measure` | No teaching lessons to measure yet. |  |  |
| `group_set_pace` | Set a sustainable pace on the class screen to see the verdict. |  |  |
| `group_day_off` | Day off: %1$s |  | Has placeholders |
| `group_no_on_day` | No lesson on %1$s |  | Has placeholders |
| `group_tab_plan` | Plan |  |  |
| `group_tab_stats` | Stats |  |  |
| `group_tips` | Tips |  |  |
| `group_details_title` | Details |  |  |
| `group_by_month_title` | Lessons by month |  |  |
| `group_pages_title` | Pages taught on %1$s %2$s |  | Has placeholders |
| `group_blocked` | one: %1$d lesson goes to tests or worksheets; pages spread over %2$s. / other: %1$d lessons go to tests or worksheets; pages spread over %2$s. |  | Plural, Has placeholders |
| `group_teaching_count` | one: %1$d lesson / other: %1$d lessons |  | Plural, Has placeholders |
| `group_more` | More options |  |  |
| `progress_strip` | Lesson %1$d of %2$d · %3$s |  | Has placeholders |
| `group_print` | Print |  |  |
| `group_make_custom_plan` | Make a custom plan |  |  |
| `group_settings_title` | Group settings |  |  |
| `group_settings_dates` | Dates |  |  |
| `group_settings_end` | End date (optional) |  |  |
| `group_settings_default_start` | School year starts: %1$s. Clear to use this date. |  | Has placeholders |
| `group_settings_default_end` | School year ends: %1$s. Clear to use this date. |  | Has placeholders |
| `group_settings_end_clamped` | This group still finishes by the school year end. |  |  |
| `group_settings_days_off` | Group days off |  |  |
| `group_settings_days_off_hint` | These additions apply only to this group. |  |  |
| `group_settings_inherited_days_off` | School year and class days off |  |  |
| `group_settings_no_days_off` | No additional days off. |  |  |
| `group_settings_skipped_pages` | Additional skipped pages |  |  |
| `group_settings_skip_hint` | These pages are added to the class skips. Clear a field to use only the class skips. |  |  |
| `group_settings_class_skip` | Class skips: %1$s |  | Has placeholders |
| `group_settings_skip_example` | 8, 24-27 |  |  |
| `group_settings_none` | None |  |  |
| `group_settings_delete` | Delete group |  |  |
| `group_settings_books` | Books |  |  |
| `group_settings_books_hint` | Class books are included unless you leave one out. Choose additional books for this group. |  |  |
| `group_settings_books_long_press` | Long-press a book to leave it out of this group or give this group its own finish-by date. |  |  |
| `group_settings_removed_book` | Not used in this group |  |  |
| `group_settings_class_date` | Class: %1$s |  |  |
| `group_settings_use_class_date` | Use class date |  |  |
| `group_settings_book_due_clamped` | This group finishes on %1$s, so the book ends then. |  |  |
| `group_settings_remove_book` | Remove from this group |  |  |
| `group_settings_remove_book_hint` | Lessons already taught stay as they are. The pages still to teach spread over the lessons left. |  |  |
| `group_settings_add_back` | Add back |  |  |
| `group_settings_keep_one_book` | A group needs at least one book that is paced. |  |  |
| `group_settings_default_book` | Class default · included |  |  |
| `group_settings_optional_book` | Optional book |  |  |
| `group_settings_new_book_hint` | This book is added to the class library and selected only for this group. Other groups can choose it in their settings. |  |  |
| `group_settings_page_range` | Enter a last page at or after the first page. |  |  |

## Group plan (ledger, custom plan, extra lessons, supplementary books)

116 entries.

| ID | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `custom_created` | Custom plan created |  |  |
| `custom_mine` | My plan |  |  |
| `custom_proposed` | Proposed |  |  |
| `custom_preview_mine` | Your plan is ready. You're teaching from the proposed plan. |  |  |
| `custom_preview_proposed` | The proposed plan, for comparison. You're teaching from your plan. |  |  |
| `custom_use_this` | Use this plan |  |  |
| `custom_now_mine` | Teaching from your plan |  |  |
| `custom_now_proposed` | Teaching from the proposed plan |  |  |
| `custom_fix_repace` | Re-pace |  |  |
| `custom_fix_tidy` | Tidy |  |  |
| `custom_fix_fit` | Fit to term |  |  |
| `custom_fix_open_lesson` | Open lesson |  |  |
| `custom_repace_all` | Re-pace untouched lessons |  |  |
| `custom_copy_to` | Copy to… |  |  |
| `custom_copied` | Plan copied to %1$s |  | Has placeholders |
| `custom_delete` | Delete my plan |  |  |
| `custom_deleted` | Custom plan deleted |  |  |
| `custom_repaced` | Plan re-paced |  |  |
| `custom_tidied` | Plan tidied |  |  |
| `custom_fitted` | Plan fitted to the term |  |  |
| `custom_hint` | Long-press a lesson to plan its pages, add a lesson, move it or re-pace from it. Drag the handle to move a lesson. Lessons you change keep an accent edge, and re-pacing never touches them. |  |  |
| `custom_plan_lesson` | Change planned pages… |  |  |
| `custom_add_before` | Add a lesson before |  |  |
| `custom_add_here` | Plan a lesson here… |  |  |
| `custom_move_earlier` | Move earlier |  |  |
| `custom_move_later` | Move later |  |  |
| `custom_repace_here` | Re-pace from here |  |  |
| `custom_remove` | Remove lesson |  |  |
| `custom_removed` | Lesson removed; its pages joined the next one |  |  |
| `custom_moved` | Lesson moved |  |  |
| `custom_free` | Free lesson |  |  |
| `custom_proposed_line` | Proposed: %1$s |  | Has placeholders |
| `custom_differs` | differs from the proposed plan |  |  |
| `custom_drag` | Drag to move lesson %1$d |  | Has placeholders |
| `custom_sheet_title` | Lesson %1$d · %2$s %3$s |  | Has placeholders |
| `custom_sheet_new` | New lesson |  |  |
| `custom_sheet_hint` | The pages this lesson will teach from each book, like 5-8, 10. Leave a book empty for none. Saving keeps the lesson as you wrote it when the plan is re-paced. |  |  |
| `custom_note` | Note |  |  |
| `custom_note_placeholder` | Revision, video, speaking activity… |  |  |
| `custom_saved` | Lesson planned |  |  |
| `custom_missing` | one: %d page left out / other: %d pages left out |  | Plural, Has placeholders |
| `custom_doubled` | one: %d page taught twice / other: %d pages taught twice |  | Plural, Has placeholders |
| `custom_stale` | one: %d page already covered / other: %d pages already covered |  | Plural, Has placeholders |
| `custom_overflow` | one: %d lesson after the term ends / other: %d lessons after the term ends |  | Plural, Has placeholders |
| `supp_tag` | Supplementary |  |  |
| `book_type` | Type |  |  |
| `book_type_student` | Student\'s Book |  |  |
| `book_type_workbook` | Workbook |  |  |
| `book_type_grammar` | Grammar Book |  |  |
| `book_type_writing` | Writing Book |  |  |
| `book_type_skills` | Skills Book |  |  |
| `book_type_reader` | Reader |  |  |
| `book_type_supplementary` | Supplementary book |  |  |
| `book_type_none` | Not set |  |  |
| `supp_switch_hint` | Never paced: you place its pages by hand in the lessons you choose, on top of the lesson or in it. |  |  |
| `supp_include` | Pages you'll use |  |  |
| `supp_include_placeholder` | 4-7, 12, 30-33 |  |  |
| `supp_include_error` | List pages or ranges, like 4-7, 12 |  |  |
| `supp_count_default` | Count in the lesson by default |  |  |
| `supp_count_default_hint` | When off, new placements come on top of the lesson. Each lesson keeps its own choice. |  |  |
| `supp_section` | Supplementary |  |  |
| `supp_in_lesson` | In the lesson |  |  |
| `supp_on_top` | On top |  |  |
| `supp_unplaced` | Not placed yet: %1$s |  | Has placeholders |
| `supp_all_placed` | Every page is placed or taught |  |  |
| `supp_page_error` | Use pages it includes and hasn't used yet |  |  |
| `supp_add_pages` | Add %1$s pages… |  | Has placeholders |
| `supp_line` | %1$s: %2$s |  | Has placeholders |
| `supp_left` | %1$s: %2$s not placed yet |  | Has placeholders |
| `supp_replace_title` | Pages to teach this lesson |  |  |
| `supp_replace_hint` | %1$s counts in this lesson. Enter what each core book will teach: pages or ranges, blank for none, or 0 to leave a book out. Pages you give up move to the next lessons. |  | Has placeholders |
| `supp_replace_apply` | Apply |  |  |
| `supp_keep_title` | Keep %1$s? |  | Has placeholders |
| `supp_keep_text` | Keep %1$s on pages %2$s, or skip it this lesson? |  | Has placeholders |
| `supp_keep` | Keep these pages |  |  |
| `supp_skip` | Don't use this lesson |  |  |
| `supp_cancel` | Cancel |  |  |
| `supp_choose_pages` | Choose pages to teach… |  |  |
| `supp_restored` | The lesson's core pages are back as they were before. Change them if you like. |  |  |
| `supp_save_failed` | Some pages can't go in this lesson. Check the fields and try again. |  |  |
| `custom_line_title` | Start from this schedule and make it yours. |  |  |
| `custom_line_body` | The proposed plan stays as your fallback. |  |  |
| `custom_line_create` | Create |  |  |
| `ledger_this_week` | This week |  |  |
| `ledger_next_week` | Next week |  |  |
| `ledger_day_off` | Day off · %1$s |  | Has placeholders |
| `ledger_test` | Test · %1$s |  | Has placeholders |
| `ledger_by_hand` | By hand |  |  |
| `ledger_supp` | + %1$s |  | Has placeholders |
| `ledger_lesson_spoken` | Lesson %1$d, %2$s. %3$s |  | Has placeholders |
| `ledger_mark_not_done` | Mark not done |  |  |
| `custom_adjust_catch_up` | Adjust catch-up… |  |  |
| `extra_add` | Add extra lesson… |  |  |
| `extra_edit` | Edit extra lesson… |  |  |
| `extra_delete` | Delete extra lesson |  |  |
| `extra_label` | Extra |  |  |
| `extra_hint` | Pages stay pinned to this date in both plans. Other lessons re-pace when you save. Core books only. |  |  |
| `extra_blocked` | A whole-lesson test blocks this extra lesson. Its pages return to the remaining plan. |  |  |
| `extra_outside` | Choose a date from the group’s next lesson through the term end. Outside this range, extra pages stay in the remaining plan. |  |  |
| `extra_done_hint` | This lesson is already taught. Use Record what I taught to change its history, or mark it not done before editing its pin. |  |  |
| `extra_after_fits` | After this: Fits |  |  |
| `extra_after_unmeasured` | After this: No regular lessons left to measure |  |  |
| `extra_saved` | Extra lesson saved |  |  |
| `extra_deleted` | Extra lesson deleted |  |  |
| `extra_unavailable` | Extra lesson on %1$s is unavailable |  | Has placeholders |
| `extra_unavailable_hint` | A whole-lesson test or the group’s date range excludes it. Its pages are back in the remaining plan. |  |  |
| `extra_after_short` | one: After this: Short by %d lesson / other: After this: Short by %d lessons |  | Plural, Has placeholders |
| `plan_weeks` | Weekly view |  |  |
| `plan_months` | Monthly view |  |  |
| `plan_this_month` | This month · %1$s |  | Has placeholders |
| `plan_previous_month` | Previous month |  |  |
| `plan_next_month` | Next month |  |  |
| `plan_choose_month` | Choose month |  |  |
| `plan_in_use` | %1$s · In use |  | Has placeholders |
| `plan_own_pace` | Own pace… |  |  |
| `plan_hide_earlier` | Hide earlier lessons |  |  |
| `plan_earlier_lessons` | one: %d earlier lesson › / other: %d earlier lessons › |  | Plural, Has placeholders |

## Group stats

37 entries.

| ID | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `stats_fix_short` | Teach %1$s to finish, or skip %2$s. |  | Has placeholders |
| `stats_more_pages` | one: about %d page more a lesson / other: about %d pages more a lesson |  | Plural, Has placeholders |
| `stats_pages` | one: %d page / other: %d pages |  | Plural, Has placeholders |
| `stats_fix_fits` | one: %1$d lesson to spare at %2$s pages a lesson. / other: %1$d lessons to spare at %2$s pages a lesson. |  | Plural, Has placeholders |
| `stats_fix_exact` | The pages fit the term exactly at %1$s pages a lesson. |  | Has placeholders |
| `stats_a_lesson` | a lesson |  |  |
| `stats_pace_track` | Pace track |  |  |
| `stats_set_position` | Change position |  |  |
| `stats_lead_book` | lead book |  |  |
| `stats_supplementary` | supplementary |  |  |
| `stats_tag` | " · %1$s" |  | Has placeholders |
| `stats_page_of` | p. %1$d of %2$d |  | Has placeholders |
| `stats_placed` | %1$d of %2$d placed |  | Has placeholders |
| `stats_taught` | one: %d page taught / other: %d pages taught |  | Plural, Has placeholders |
| `stats_left_rate` | one: %1$d page left · %2$s a lesson / other: %1$d pages left · %2$s a lesson |  | Plural, Has placeholders |
| `stats_behind_lead` | one: %1$d lesson behind the lead book · in step at p. %2$d / other: %1$d lessons behind the lead book · in step at p. %2$d |  | Plural, Has placeholders |
| `stats_behind_others` | one: %1$d lesson behind the other books / other: %1$d lessons behind the other books |  | Plural, Has placeholders |
| `stats_month_spoken` | one: %1$s: %2$d lesson / other: %1$s: %2$d lessons |  | Plural, Has placeholders |
| `stats_more` | More stats › |  |  |
| `stats_fewer` | Fewer stats |  |  |
| `stats_lessons_planned` | Lessons planned |  |  |
| `stats_plan_rate` | Planned pace |  |  |
| `stats_plan_end` | Plan ends |  |  |
| `stats_term_end` | Term ends |  |  |
| `stats_own_pace` | Set my own pace… |  |  |
| `stats_own_pace_title` | My own pace |  |  |
| `stats_own_pace_hint` | Set pages per lesson across core books. Lessons you changed by hand stay as they are. You can also open this by holding Pace needed. |  |  |
| `stats_own_pace_error` | Enter a positive pace. Try a higher number if the plan is too long. |  |  |
| `stats_own_pace_field` | Pages per lesson |  |  |
| `stats_own_pace_preview` | Ends %1$s |  | Has placeholders |
| `stats_own_pace_overflow` | one: %d planned lesson falls after the term. / other: %d planned lessons fall after the term. |  | Plural, Has placeholders |
| `stats_covered_short` | one: Your plan covers %1$d of %2$d short lesson. / other: Your plan covers %1$d of %2$d short lessons. |  | Plural, Has placeholders |
| `stats_beyond_term` | Beyond the term |  |  |
| `stats_plan_position` | On plan: page %1$d; you are at page %2$d |  | Has placeholders |
| `stats_no_end` | No dated lessons |  |  |
| `stats_my_plan` | My plan |  |  |
| `stats_proposed` | Proposed |  |  |

## Work: homework, tests and worksheets

51 entries.

| ID | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `work_title` | Work |  |  |
| `work_all` | All |  |  |
| `work_homework` | Homework |  |  |
| `work_test` | Test |  |  |
| `work_worksheet` | Worksheet |  |  |
| `work_tests` | Tests |  |  |
| `work_worksheets` | Worksheets |  |  |
| `work_add` | Add assignment |  |  |
| `work_edit` | Edit assignment |  |  |
| `work_empty` | No assignments here |  |  |
| `work_empty_hint` | Add homework, a test or a worksheet for this group. |  |  |
| `work_this_week` | This week |  |  |
| `work_later` | Later |  |  |
| `work_undated` | No date |  |  |
| `work_earlier` | Earlier |  |  |
| `work_set_on` | Set-on lesson (optional) |  |  |
| `work_due` | Due lesson (optional) |  |  |
| `work_set_date` | Set %1$s |  | Has placeholders |
| `work_due_date` | Due %1$s |  | Has placeholders |
| `work_date_error` | Due must be on or after the set-on date. |  |  |
| `work_not_lesson` | There is no lesson on one of these dates. You can keep the date or choose a lesson day. |  |  |
| `work_saved` | Assignment saved |  |  |
| `work_deleted` | Assignment removed |  |  |
| `work_progress` | Progress |  |  |
| `work_due_soon` | Due soon |  |  |
| `work_no_due` | No dated assignments coming up. |  |  |
| `work_due_today` | Homework due today: %1$s |  | Has placeholders |
| `work_set_today` | Set today: %1$s |  | Has placeholders |
| `work_moved` | Assignments have moved to each group. |  |  |
| `work_book_left` | one: %1$s · %2$d page left / other: %1$s · %2$d pages left |  | Plural, Has placeholders |
| `work_needs_date` | Choose a due lesson to reserve time in class. |  |  |
| `work_worksheet_lesson` | Worksheet · %1$s |  | Has placeholders |
| `work_open_stats` | Open book stats |  |  |
| `work_unselected_books` | Pages from books no longer selected for this group are kept. Choose the book in Group settings to edit them. |  |  |
| `homework_add` | Add homework… |  |  |
| `homework_edit_named` | Edit homework: %1$s… |  | Has placeholders |
| `homework_delete_named` | Delete homework: %1$s |  | Has placeholders |
| `homework_content` | Homework content |  |  |
| `homework_pages_notes` | Pages or notes |  |  |
| `homework_row` | HW: %1$s |  | Has placeholders |
| `homework_due_short` | due %1$s |  | Has placeholders |
| `homework_schedule_title` | Schedule it in class? |  |  |
| `homework_also_title` | Also set it as homework? |  |  |
| `homework_schedule_hint` | Choose a lesson for %1$s. The homework is already saved. |  | Has placeholders |
| `homework_also_hint` | Set %1$s before its class lesson. The class assignment is already saved. |  | Has placeholders |
| `homework_reserves_lesson` | This reserves the whole lesson for the test or worksheet. |  |  |
| `homework_lesson` | Lesson |  |  |
| `homework_schedule` | Schedule in class |  |  |
| `homework_set` | Set as homework |  |  |
| `homework_skip` | Skip |  |  |
| `homework_linked` | Class work and homework linked |  |  |

## Pacing overview

19 entries.

| ID | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `overview_no_classes` | No classes yet |  |  |
| `overview_empty_hint` | Add a class to see how its groups are progressing. |  |  |
| `overview_open_classes` | Open classes |  |  |
| `overview_class` | Class |  |  |
| `overview_no_groups` | No groups in this class yet. |  |  |
| `overview_group` | Group |  |  |
| `overview_no_lessons` | No lessons left in this term. |  |  |
| `overview_behind_suffix` | one:  · %d behind / other:  · %d behind |  | Plural, Has placeholders |
| `overview_class_behind` | one: %d behind / other: %d behind |  | Plural, Has placeholders |
| `overview_all_on_pace` | All on pace |  |  |
| `overview_next_on` | Next %1$s %2$s |  | Has placeholders |
| `overview_sort` | Groups: %1$s |  | Has placeholders |
| `overview_sort_behind` | most behind first |  |  |
| `overview_sort_name` | by name |  |  |
| `overview_sort_next` | by next lesson |  |  |
| `overview_problems` | Problems in this year |  |  |
| `overview_on_pace_link` | one: %d on pace › / other: %d on pace › |  | Plural, Has placeholders |
| `overview_classes_on_pace` | one: %d class all on pace › / other: %d classes all on pace › |  | Plural, Has placeholders |
| `overview_hide_on_pace` | Hide groups on pace |  |  |

## School year

51 entries.

| ID | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `year_class_all` | %1$s · all groups |  | Has placeholders |
| `year_group_only` | %1$s only |  | Has placeholders |
| `year_all_classes` | All classes |  |  |
| `year_all_groups` | All groups |  |  |
| `year_removed_group` | removed group |  |  |
| `year_removed` | Removed “%1$s” |  | Has placeholders |
| `year_already_there` | Already there |  |  |
| `year_holidays_hint` | Greek holidays are computed from Orthodox Easter for every calendar year the school year touches. Check them against your school’s calendar before relying on them. |  |  |
| `year_school_year` | School year |  |  |
| `year_name` | Name |  |  |
| `year_starts` | Starts |  |  |
| `year_ends` | Ends |  |  |
| `year_not_set` | Not set |  |  |
| `year_term_hint` | Every class paces up to the end date. Each group tracks its own next lesson; the start is only the default. |  |  |
| `year_delete` | Delete this school year |  |  |
| `year_days_off` | Days off |  |  |
| `year_no_days_off` | No days off yet. |  |  |
| `year_day_off` | Day off |  |  |
| `year_add_day_off` | Add a day off |  |  |
| `year_edit_day_off` | Edit day off |  |  |
| `year_term_starts` | Term starts |  |  |
| `year_term_ends` | Term ends |  |  |
| `year_save` | Save |  |  |
| `year_date` | Date |  |  |
| `year_add` | Add |  |  |
| `year_reason` | Reason |  |  |
| `year_reason_example` | Christmas break |  |  |
| `year_from` | From |  |  |
| `year_to_optional` | To (optional) |  |  |
| `year_applies_to` | Applies to |  |  |
| `year_classes_hint` | A class is a set of books taught to one or more groups. Each group keeps its own days and place in the books. |  |  |
| `year_first_class` | Add your first class |  |  |
| `year_first_class_hint` | Give it a name, then add books and set your groups’ lesson days. |  |  |
| `year_new_class` | New class |  |  |
| `year_class` | Class |  |  |
| `year_no_groups` | No groups |  |  |
| `year_defaults_hint` | New classes take the lesson length and pace from Settings. |  |  |
| `year_class_example` | B1 Senior |  |  |
| `year_lessons_across` | Lessons left across %1$s · %2$s |  | Has placeholders |
| `year_holidays_added` | one: %d holiday added / other: %d holidays added |  | Plural, Has placeholders |
| `year_problems` | one: %d problem / other: %d problems |  | Plural, Has placeholders |
| `year_groups` | one: %d group / other: %d groups |  | Plural, Has placeholders |
| `year_books` | one: %d book / other: %d books |  | Plural, Has placeholders |
| `year_term` | Term |  |  |
| `year_done` | Done |  |  |
| `year_dates_not_set` | Dates not set |  |  |
| `year_date_warning` | Set a start and end date, with the end on or after the start. |  |  |
| `year_edit` | Edit |  |  |
| `year_weeks` | one: %d week / other: %d weeks |  | Plural, Has placeholders |
| `year_days_off_count` | one: %d day off / other: %d days off |  | Plural, Has placeholders |
| `year_add_greek` | Add Greek public holidays |  |  |

## Units

51 entries.

| ID | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `units_title` | Book units |  |  |
| `units_enable` | Enable book units? |  |  |
| `units_on` | On |  |  |
| `units_off` | Off |  |  |
| `units_creation_hint` | Add books, then choose their units and links in Book units. |  |  |
| `units_rule` | Finish each unit in every linked book before starting the next. Unlinked books keep their own pacing. |  |  |
| `units_step_lead` | 1 · Choose the lead book |  |  |
| `units_step_starts` | 2 · Set the units |  |  |
| `units_step_links` | 3 · Link books (optional) |  |  |
| `units_no_books` | Add the books you teach from first. |  |  |
| `units_starts_hint` | Enter each unit’s first page. The first unit starts on the book’s first page; the final unit covers its last page. |  |  |
| `units_needs_starts` | Add unit start pages |  |  |
| `units_count` | %1$d units |  | Has placeholders |
| `units_add` | Add units |  |  |
| `units_number` | Number of units |  |  |
| `units_space_evenly` | Space evenly |  |  |
| `units_start_page` | Unit %1$d start page |  | Has placeholders |
| `units_end_page` | Unit %1$d ends on page %2$d |  | Has placeholders |
| `units_start_error` | Start with page %1$d, then enter a higher page for every unit, up to page %2$d. |  | Has placeholders |
| `units_links_hint` | Linked books finish the same unit together. Each needs the same number of units as the lead book. |  |  |
| `units_choose_lead_first` | Choose a lead book and add its units first. |  |  |
| `units_matches` | Matches the lead book (%1$d units) |  | Has placeholders |
| `units_mismatch` | Doesn’t match the lead book (%1$d units). Add units or leave unlinked. |  | Has placeholders |
| `units_dont_link` | Don’t link |  |  |
| `units_links_later` | You can change links here later. |  |  |
| `units_group_setting` | Book units for this group |  |  |
| `units_follow_class` | Follow class setting |  |  |
| `units_override_warning` | This overrides the class setting. |  |  |
| `units_class_off` | Book units are off for the class. This group’s choice is kept for when the class enables them again. |  |  |
| `units_revision_title` | Unit %1$d revision |  | Has placeholders |
| `units_revision_format` | Revision format |  |  |
| `units_revision_pages` | Pages of a book |  |  |
| `units_revision_none` | No revision |  |  |
| `units_revision_lesson` | Future lesson |  |  |
| `units_revision_book` | Supplementary book |  |  |
| `units_revision_no_books` | Add a book in Class settings to use its pages. |  |  |
| `units_revision_pages_error` | Enter pages included in the book. |  |  |
| `units_revision_unscheduled` | No future lesson is available. Your revision choice will be saved unscheduled; add a lesson or extend the term. |  |  |
| `units_revision_remove` | Remove revision |  |  |
| `units_revision_add` | Add a revision |  |  |
| `units_position` | Unit %1$d of %2$d |  | Has placeholders |
| `units_lesson_position` | Unit %1$d · lesson %2$d of %3$d |  | Has placeholders |
| `units_remaining` | %1$s · %2$d pages left |  | Has placeholders |
| `units_holds` | %1$s holds the next unit back |  | Has placeholders |
| `units_fewer` | Fewer units |  |  |
| `units_more` | More units |  |  |
| `units_minus` | − |  |  |
| `units_revision_saved` | Revision saved |  |  |
| `units_revision_removed` | Revision removed |  |  |
| `units_revision_mark` | ◇ |  |  |
| `units_revision_mark_done` | ◆ |  |  |

## Warnings and problems

27 entries.

| ID | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `warn_catch_up` | Catch up |  |  |
| `warn_catch_up_title` | Catch up |  |  |
| `warn_apply_catch_up` | Apply catch-up |  |  |
| `warn_on_pace` | This group is on pace. Nothing to catch up. |  |  |
| `warn_boost` | +%1$d a lesson |  | Has placeholders |
| `warn_catch_up_plan` | Teach %1$s pages a lesson until then, and you're back on pace by %2$s %3$s. |  | Has placeholders |
| `warn_infeasible` | Even at this pace the term's %1$d lessons are not enough. Take a bigger step, skip pages or set some for homework. |  | Has placeholders |
| `warn_bigger_step` | Bigger step |  |  |
| `warn_writes_custom` | This goes into your custom plan (one is made from the proposed plan if you have none). The proposed plan stays as it is, and lessons you planned by hand don't change. |  |  |
| `warn_first_lessons` | First lessons |  |  |
| `warn_fold` | Bring in step |  |  |
| `warn_fold_title` | Bring %1$s back in step |  | Has placeholders |
| `warn_apply_fold` | Apply |  |  |
| `warn_book_behind` | %1$s is %2$s lessons behind the others |  | Has placeholders |
| `warn_caught_up` | Catch-up added to your plan |  |  |
| `warn_folded` | %1$s brought back in step |  | Has placeholders |
| `warn_behind` | one: %1$d page more than %2$s pages a lesson fits in the term. / other: %1$d pages more than %2$s pages a lesson fit in the term. |  | Plural, Has placeholders |
| `warn_lessons_word` | one: lesson to catch up / other: lessons to catch up |  | Plural |
| `warn_over` | one: Over %d lesson / other: Over %d lessons |  | Plural, Has placeholders |
| `warnings_count` | one: %d warning / other: %d warnings |  | Plural, Has placeholders |
| `warnings_title` | Warnings · %1$s |  | Has placeholders |
| `warnings_none` | Nothing needs your attention. |  |  |
| `warnings_book_behind` | one: %1$s is %2$d lesson behind / other: %1$s is %2$d lessons behind |  | Plural, Has placeholders |
| `warnings_book_hint` | The other books are further on. |  |  |
| `warnings_plan_hint` | In your custom plan. |  |  |
| `warnings_edited_hint` | In a lesson you edited, so Tidy leaves it as it is. |  |  |
| `warnings_overflow_stuck` | No lesson you haven't edited is free in the term, so Fit to term has nowhere to put them. |  |  |
| `warnings_overflow_edited` | one: %d of them you edited, so it stays as it is. / other: %d of them you edited, so they stay as they are. |  | Plural, Has placeholders |
| `warn_repace_keeps_catch_up` | Re-pacing untouched lessons keeps your chosen catch-up pace. |  |  |
| `warnings_repace_to_fit` | Re-pace to fit |  |  |

## Print

12 entries.

| ID | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `print_title` | Print or save PDF |  |  |
| `print_print` | Print |  |  |
| `print_none` | No lessons left to print. |  |  |
| `print_class` | Class |  |  |
| `print_margins` | Margins |  |  |
| `print_margin_hint` | %1$s: %2$d mm on every side. Each group starts on a new page; the system print dialog then offers paper size, orientation and Save as PDF. |  | Has placeholders |
| `print_narrow` | Narrow |  |  |
| `print_normal` | Normal |  |  |
| `print_wide` | Wide |  |  |
| `print_only_group` | This group: %1$s · %2$s |  | Has placeholders |
| `print_whole_class` | Every group in %1$s |  | Has placeholders |
| `print_whole_year` | Every group this year |  |  |

## Settings, palette and updates

72 entries.

| ID | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `settings_theme_system` | System |  |  |
| `settings_theme_light` | Light |  |  |
| `settings_theme_dark` | Dark |  |  |
| `settings_theme` | Theme |  |  |
| `settings_new_classes` | New classes |  |  |
| `settings_class_defaults` | Class defaults |  |  |
| `settings_defaults_hint` | Used when you add a class. Existing classes keep their own values, which you change on the class screen. |  |  |
| `settings_help` | Help |  |  |
| `settings_your_name` | Your name |  |  |
| `settings_name_hint` | Home greets you by it. Leave it empty and Home says Hello. |  |  |
| `settings_show_tips` | Show tips again |  |  |
| `settings_show_tips_detail` | Tips you closed come back where they first appeared. |  |  |
| `settings_tips_reset` | Tips will show again |  |  |
| `settings_getting_started` | Getting started |  |  |
| `settings_getting_started_detail` | What the app does, and the steps to a first schedule |  |  |
| `settings_about` | About |  |  |
| `settings_local_storage` | School years are saved on this device and included in Android backups, along with these settings. |  |  |
| `settings_suite` | Part of the EduSuite apps, alongside Course Invoicer. |  |  |
| `settings_credits` | Outfit typeface: © The Outfit Project Authors, SIL Open Font License 1.1. AndroidX: Apache License 2.0. Material Symbols Rounded: Google, Apache License 2.0. |  |  |
| `settings_copyright` | © 2026 Dimitrios Tsampallas. All rights reserved. |  |  |
| `settings_defaults_title` | New class defaults |  |  |
| `settings_done` | Done |  |  |
| `settings_lesson_length` | Lesson length (min) |  |  |
| `settings_defaults_saved` | Changes are saved as you type and apply to new classes only. |  |  |
| `settings_backup` | Back up and restore |  |  |
| `settings_export` | Export… |  |  |
| `settings_import` | Import… |  |  |
| `settings_backup_hint` | Export saves every school year to a file, in Downloads, Google Drive or wherever your phone offers. Import reads it back, on this phone or another, and also reads the Coursebook Pacer web page's export. Settings aren't included. |  |  |
| `settings_uninstall_hint` | Export before you uninstall the app or change phones: uninstalling deletes the school years. |  |  |
| `settings_replace` | Replace all |  |  |
| `settings_add` | Add |  |  |
| `settings_cancel` | Cancel |  |  |
| `settings_defaults_summary` | %1$d min · %2$s pages per lesson |  | Has placeholders |
| `settings_version` | %1$s %2$s |  | Has placeholders |
| `settings_export_saved` | Saved %1$s |  | Has placeholders |
| `settings_export_saved_to` | Saved %1$s to %2$s |  | Has placeholders |
| `settings_save_error` | Couldn't save the file. Try another place. |  |  |
| `settings_import_empty` | That file holds no school years. |  |  |
| `settings_import_newer` | That file comes from a newer version of %1$s. Update the app, then import it. |  | Has placeholders |
| `settings_import_too_large` | This backup is too large to open (%1$d MB; the limit is %2$d MB). |  | Has placeholders |
| `settings_import_unreadable` | Couldn't read that file. Choose one exported from %1$s or from the Coursebook Pacer web page. |  | Has placeholders |
| `settings_name_separator` | ", " |  |  |
| `settings_no_years` | There are no school years on this phone yet. |  |  |
| `settings_import_body` | %1$s.<br><br>%2$s You can undo it straight after. |  | Has placeholders |
| `settings_no_picker` | This phone has no file picker. |  |  |
| `settings_year_count` | one: %d school year / other: %d school years |  | Plural, Has placeholders |
| `settings_import_added` | one: Added %d school year / other: Added %d school years |  | Plural, Has placeholders |
| `settings_import_replaced` | one: Replaced your school years with %d school year from the file / other: Replaced your school years with %d school years from the file |  | Plural, Has placeholders |
| `settings_more_years` | one: " and %d more" / other: " and %d more" |  | Plural, Has placeholders |
| `settings_import_explanation` | one: Add puts them beside the %d school year on this phone. Replace all deletes it and keeps only the file's. / other: Add puts them beside the %d school years on this phone. Replace all deletes those and keeps only the file's. |  | Plural, Has placeholders |
| `settings_import_title` | one: Import %d school year? / other: Import %d school years? |  | Plural, Has placeholders |
| `settings_palette` | Palette |  |  |
| `palette_bone` | Freetown |  |  |
| `palette_bone_body` | Bone paper, chalkboard green and ballpoint blue |  |  |
| `palette_saffron` | Arusha |  |  |
| `palette_saffron_body` | Graphite, pencil yellow and teal |  |  |
| `palette_oxford` | Oxford |  |  |
| `palette_oxford_body` | White paper, flag navy and red |  |  |
| `palette_selected` | Selected |  |  |
| `palette_unselected` | Not selected |  |  |
| `palette_description` | %1$s. %2$s |  | TalkBack, Has placeholders |
| `settings_updates` | Updates |  |  |
| `updates_available` | Version %1$s is available |  | Has placeholders |
| `updates_download` | Download |  |  |
| `updates_first_time` | The first time, Android may ask you to allow installs from your browser. |  |  |
| `updates_see_changes` | See all changes |  |  |
| `updates_auto` | Check for updates automatically |  |  |
| `updates_auto_hint` | Once a day, Course Pacer asks GitHub for the latest version. Nothing about you or your school years is sent. |  |  |
| `updates_check_now` | Check now |  |  |
| `updates_checking` | Checking… |  |  |
| `updates_up_to_date` | Course Pacer is up to date. |  |  |
| `updates_failed` | Couldn't check for updates. Check your connection and try again. |  |  |
| `updates_installed` | Installed version %1$s |  | Has placeholders |
| `updates_no_browser` | No browser found. The download link was copied. |  |  |
| `updates_link_label` | Course Pacer download link |  |  |

## Recovery and notices

7 entries.

| ID | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `recovery_saved` | Saved a copy of the unreadable file. |  |  |
| `recovery_title` | Your school years couldn't be read |  |  |
| `recovery_original_safe` | Your original file is safe |  |  |
| `recovery_hint` | Save a copy to keep it somewhere you can reach. You can also restore an earlier export from Settings → Back up and restore. |  |  |
| `recovery_save` | Save a copy… |  |  |
| `recovery_close` | Close |  |  |
| `recovery_explanation` | %1$s started with no school years because the saved file could not be read. The original file is kept on this phone as it was. |  | Has placeholders |

## Shared components

25 entries.

| ID | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `component_fix` | Fix |  |  |
| `component_not_set` | Not set |  |  |
| `component_date_description` | %1$s, %2$s |  | TalkBack, Has placeholders |
| `component_remove_what` | Remove %1$s |  | Has placeholders |
| `component_add` | Add |  |  |
| `component_options` | Options |  |  |
| `component_pace` | Pace you can sustain |  |  |
| `component_ok` | OK |  |  |
| `component_clear` | Clear |  |  |
| `component_cancel` | Cancel |  |  |
| `component_remove` | Remove |  |  |
| `component_more_options` | More options |  |  |
| `component_expanded` | Expanded |  |  |
| `component_collapsed` | Collapsed |  |  |
| `component_previous_week` | Previous week |  |  |
| `component_next_week` | Next week |  |  |
| `component_today` | Today |  |  |
| `component_lesson` | Lesson |  |  |
| `component_week_range` | %1$s – %2$s |  | Has placeholders |
| `component_day_description` | %1$s %2$s %3$s |  | TalkBack, Has placeholders |
| `component_invalid_value` | Enter a valid value. |  |  |
| `component_integer_range` | Enter a whole number from %1$d to %2$d. |  | Has placeholders |
| `component_pace_invalid` | Enter a finite number of pages, zero or greater. |  |  |
| `component_save` | Save |  |  |
| `component_saved` | Saved |  |  |

## Strings written in Kotlin

These are not in `strings.xml` because the engine, the saved data and the printed sheet are kept independent of Android resources (`CLAUDE.md`, UI copy and icons). `{x}` stands for a value filled in at run time. Where a message is shown on screen, it appears in the warning lists and sheets.

### Engine warnings (shown in the problems lists and on screen)

Written once in the engine, so they read the same everywhere. `Nav.problems` matches some of them by their opening words to choose the screen that fixes them, so changing the start of a message also needs a code change there.

| Where | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `app/src/main/java/gr/edusuite/pacer/engine/Pacer.kt:411` | Set both a term start and a term end. |  | Matched by `Nav.fixFor` |
| `app/src/main/java/gr/edusuite/pacer/engine/Pacer.kt:412` | The term ends before it starts. |  | Matched by `Nav.fixFor` |
| `app/src/main/java/gr/edusuite/pacer/engine/Pacer.kt:413` | Add at least one group. |  | Matched by `Nav.fixFor` |
| `app/src/main/java/gr/edusuite/pacer/engine/Pacer.kt:415` | {group} has no teaching days selected. |  | Matched by `Nav.fixFor` |
| `app/src/main/java/gr/edusuite/pacer/engine/Pacer.kt:416` | {group} has finished the term. |  | Matched by `Nav.fixFor` |
| `app/src/main/java/gr/edusuite/pacer/engine/Pacer.kt:418` | Add at least one book. |  | Matched by `Nav.fixFor` |
| `app/src/main/java/gr/edusuite/pacer/engine/Pacer.kt:424` | {book}: list the pages you'll use. |  | Supplementary book |
| `app/src/main/java/gr/edusuite/pacer/engine/Pacer.kt:427` | {book}: the last page is before the first. |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Pacer.kt:429` | {book}: the finish-by date falls after the term ends, so it has no effect. |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Pacer.kt:436` | {book}: {group} has no lesson left before the finish-by date. All {n} pages land in the next lesson. |  | Two sentences joined |
| `app/src/main/java/gr/edusuite/pacer/engine/Pacer.kt:445` | {group}'s plan leaves {n} page(s) out. |  | "page" or "pages" |
| `app/src/main/java/gr/edusuite/pacer/engine/Pacer.kt:446` | {group}'s plan has {n} lesson(s) after the term ends. |  | "lesson" or "lessons" |
| `app/src/main/java/gr/edusuite/pacer/engine/Lead.kt:85` | {book}: its units don't match {lead} ({m} vs {n}), so it follows page by page. |  | Lead book units |
| `app/src/main/java/gr/edusuite/pacer/engine/Units.kt:60` | Choose a lead book to plan units. |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Units.kt:62` | {lead}: enter ascending, distinct unit starts beginning on page {first}. |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Units.kt:66` | {book}: enter ascending, distinct unit starts beginning on page {first}; {n} pages remain unscheduled. |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Units.kt:67` | {book}: its units don't match {lead} ({a} vs {b}); {n} pages remain unscheduled. |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Units.kt:103` | Unit {n}: no lesson window is available; {n} pages remain unscheduled. Adjust revision dates, add lessons or extend the term. |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Units.kt:106` | {book}: unit {n} cannot finish before its finish-by date while linked unit boundaries are kept. |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Units.kt:113` | Unit {n}: revision has no available lesson. Choose a future lesson. |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Units.kt:211` | Unit {n}: teaching on {date} conflicts with its revision on {date}. Manual lessons are preserved; adjust lesson pages or the revision date. |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Units.kt:223` | Unit {n}: {books} still has pages when a later unit starts on {date}. Manual lessons are preserved; adjust their pages or dates. |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Units.kt:230` | Unit {n}: {books} holds the next unit back. |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Pacer.kt:421` | {group}: {unit message} |  | Prefix added to each unit message above |
| `app/src/main/java/gr/edusuite/pacer/engine/Custom.kt:105` | Choose a positive, finite pace. |  | Own pace |
| `app/src/main/java/gr/edusuite/pacer/engine/Custom.kt:114` | This pace needs too many lessons. |  | Own pace |
| `app/src/main/java/gr/edusuite/pacer/engine/Custom.kt:117` | This pace needs too many lessons. |  | Own pace |

### Problem lists built from the above

`Nav.problems` adds its own lines and prefixes.

| Where | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `app/src/main/java/gr/edusuite/pacer/ui/Nav.kt:155` | Add at least one class. |  |  |
| `app/src/main/java/gr/edusuite/pacer/ui/Nav.kt:158` | {class or "Class"}: {message} |  | The word "Class" stands in for a blank class name |
| `app/src/main/java/gr/edusuite/pacer/ui/ClassScreens.kt:221` | Add at least one book. |  | Matching key for `Nav.problems`; also the key that picks the guide card |
| `app/src/main/java/gr/edusuite/pacer/ui/ClassScreens.kt:222` | Add at least one group. |  | Matching key for `Nav.problems`; also the key that picks the guide card |

### Printed sheet and exports

The printed HTML (`PrintSheet`) and the tab-separated copy (`Format.kt`). The two lines about arithmetic are one sentence shared by the welcome guide and the printed note.

| Where | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `app/src/main/java/gr/edusuite/pacer/ui/PrintSheet.kt:36` | {title} schedule |  | Print job name |
| `app/src/main/java/gr/edusuite/pacer/ui/PrintSheet.kt:66` | {plan} — {group} |  | Heading of each printed group |
| `app/src/main/java/gr/edusuite/pacer/ui/PrintSheet.kt:67` | {n} lessons |  | Printed summary line |
| `app/src/main/java/gr/edusuite/pacer/ui/PrintSheet.kt:68` |  ({n} for tests) |  | Printed summary line |
| `app/src/main/java/gr/edusuite/pacer/ui/PrintSheet.kt:69` |  · {n} pages · {rate} per teaching lesson · {date} to {date} |  | Printed summary line |
| `app/src/main/java/gr/edusuite/pacer/ui/Guide.kt:131-136` | The schedule is arithmetic, not advice: it spreads each book's pages evenly over the lessons left. It can't know how your class is really doing, so go by what you actually teach. |  | `PRINT_NOTE`, printed under every group; the same sentence without the final stop is `ARITHMETIC` |
| `app/src/main/java/gr/edusuite/pacer/ui/PrintSheet.kt:75` | # · Date |  | Printed table headings |
| `app/src/main/java/gr/edusuite/pacer/ui/PrintSheet.kt:77` | Note |  | Printed table heading, only when a lesson has a note |
| `app/src/main/java/gr/edusuite/pacer/ui/PrintSheet.kt:92` | [Extra] |  | Printed beside an extra lesson |
| `app/src/main/java/gr/edusuite/pacer/engine/Format.kt:32` | #, Date, Day, Note |  | Headings of the tab-separated copy; Note only when needed |
| `app/src/main/java/gr/edusuite/pacer/engine/Format.kt:40` |  [Extra] |  | Copy of an extra lesson |
| `app/src/main/java/gr/edusuite/pacer/data/Backup.kt:22` | course-pacer-{date}.json |  | Suggested file name for an export |
| `app/src/main/java/gr/edusuite/pacer/ui/SetAside.kt:61` | course-pacer-unreadable-{date}.json |  | Suggested file name for the saved copy of an unreadable file |

### Greek holidays added to the days off

Names of the holidays that "Add Greek holidays" puts in the list of days off.

| Where | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `app/src/main/java/gr/edusuite/pacer/engine/Holidays.kt:22` | New Year's Day |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Holidays.kt:23` | Epiphany |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Holidays.kt:24` | Clean Monday |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Holidays.kt:25` | Independence Day |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Holidays.kt:26` | Good Friday |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Holidays.kt:27` | Easter Monday |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Holidays.kt:28` | Labour Day |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Holidays.kt:29` | Holy Spirit Monday |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Holidays.kt:30` | Ohi Day |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Holidays.kt:31` | Polytechnic Day |  |  |
| `app/src/main/java/gr/edusuite/pacer/engine/Holidays.kt:32` | Christmas Day |  |  |

### Default names given to new items

Stored with the item, so a teacher sees them until they rename it.

| Where | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `app/src/main/java/gr/edusuite/pacer/ui/PacerViewModel.kt:417` | School year |  | New school year |
| `app/src/main/java/gr/edusuite/pacer/data/JsonCodec.kt:114` | School year |  | Year read without a name |
| `app/src/main/java/gr/edusuite/pacer/ui/PacerViewModel.kt:459` | Day off |  | New day off |
| `app/src/main/java/gr/edusuite/pacer/data/JsonCodec.kt:251` | Day off |  | Day off read without a label |
| `app/src/main/java/gr/edusuite/pacer/ui/PacerViewModel.kt:486` | Group A |  | First group of a new class |
| `app/src/main/java/gr/edusuite/pacer/ui/PacerViewModel.kt:487` | Class {n} |  | New class |
| `app/src/main/java/gr/edusuite/pacer/ui/PacerViewModel.kt:542` | Group {letter} |  | New group (the prefix is "Group ") |
| `app/src/main/java/gr/edusuite/pacer/ui/PacerViewModel.kt:767` | Book {n} |  | New book |
| `app/src/main/java/gr/edusuite/pacer/ui/PacerViewModel.kt:780` | Book {n} |  | New supplementary book |
| `app/src/main/java/gr/edusuite/pacer/data/Migration.kt:14` | Main class |  | Class made from an imported web-page plan |
| `app/src/main/java/gr/edusuite/pacer/data/Backup.kt:41` | {year} (imported) |  | Imported year whose name clashes |
| `app/src/main/java/gr/edusuite/pacer/data/Backup.kt:43` | {year} (imported {n}) |  | Imported year whose name clashes again |

### Spoken text built in Kotlin

Not a resource string, but read by TalkBack.

| Where | Current text | Your proposal | Notes |
| --- | --- | --- | --- |
| `app/src/main/java/gr/edusuite/pacer/ui/GroupStats.kt:268` | {book}. {plan description}. {under} |  | Spoken for a book on the Stats pace track; the pieces come from resource strings |

Debug-only log messages in `PlanStore.kt` are not shown to teachers and are left out.
