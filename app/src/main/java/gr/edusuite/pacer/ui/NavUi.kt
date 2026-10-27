package gr.edusuite.pacer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import gr.edusuite.pacer.R
import gr.edusuite.pacer.engine.fmt
import gr.edusuite.pacer.engine.BookLag
import gr.edusuite.pacer.engine.GroupReport
import gr.edusuite.pacer.engine.plain
import gr.edusuite.pacer.model.AppData
import gr.edusuite.pacer.model.SchoolYear
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space

/*
 * Navigation (design decisions 3, 4 and 9): the ☰ menu, the group switcher and the Warnings sheet. What they list comes
 * from NavModel.kt; where they go follows Nav.goTo.
 */

/** "Short by 2 lessons" on a pale pill, or a muted "On pace"; nothing when there is nothing to measure. */
@Composable
fun StatusLabel(short: Int?, modifier: Modifier = Modifier) {
    when {
        short == null -> Unit
        short > 0 -> Tag(pluralStringResource(R.plurals.status_short, short, short), modifier, warn = true)
        else -> Text(stringResource(R.string.status_on_pace), modifier, style = MaterialTheme.typography.labelSmall, color = Pacer.colors.muted)
    }
}

/** "Find a class or group": a filled pill with a search symbol and a clear button once there is text. */
@Composable
fun SearchField(query: String, onQuery: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = Pacer.colors
    TextField(
        value = query,
        onValueChange = onQuery,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        placeholder = { Text(stringResource(R.string.nav_search), color = c.muted) },
        leadingIcon = { AppIcon(AppIcons.Search) },
        trailingIcon = if (query.isEmpty()) null else {
            { IconButton(onClick = { onQuery("") }) { AppIcon(AppIcons.Close, contentDescription = stringResource(R.string.nav_clear_search)) } }
        },
        singleLine = true,
        shape = CircleShape,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = searchFieldColors(),
    )
}

/** Search shares the palette foreground boundary with other input fields. */
@Composable
internal fun searchFieldColors(): androidx.compose.material3.TextFieldColors {
    val c = Pacer.colors
    return TextFieldDefaults.colors(
        cursorColor = c.accentText,
        focusedContainerColor = c.surfaceContainer, unfocusedContainerColor = c.surfaceContainer,
        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
        focusedLeadingIconColor = c.muted, unfocusedLeadingIconColor = c.muted,
    )
}

/** "B1 Pre-Lower · Wed–Fri", with the usual stand-ins for blank names. */
@Composable
private fun groupLabel(className: String, groupName: String): String = stringResource(
    R.string.nav_class_group,
    className.ifBlank { stringResource(R.string.app_class) },
    groupName.ifBlank { stringResource(R.string.app_group) },
)

/**
 * The ☰ menu (decision 3): search over the year's classes and groups, then the same five items in the same order:
 * Home, Pacing overview with how many groups are behind, Classes & Groups, School year and Settings. Classes & Groups
 * opens where the teacher is ("You are here"): the classes, one class's groups, or one group's places, each level
 * hiding the ones above it. [onClose] closes the drawer before it moves. With [permanent] (a tablet 840dp wide or
 * more, pinned) it is the always-open drawer beside the screens; [pin] then shows the pin that lets it collapse.
 * [opening] is true while the modal drawer opens, so the list starts at the teacher's own place each time.
 */
@Composable
fun AppDrawer(
    data: AppData,
    screen: Screen,
    vm: PacerViewModel,
    onClose: () -> Unit,
    permanent: Boolean = false,
    updateBadge: Boolean = false,
    opening: Boolean = false,
    plansStandIn: Boolean = false,
    pin: ((Boolean) -> Unit)? = null,
    pinned: Boolean = true,
) {
    val c = Pacer.colors
    var query by rememberSaveable { mutableStateOf("") }
    val year = data.year
    val here = menuLevelFor(screen, data.activeClass)
    var expanded by rememberSaveable { mutableStateOf(here != null) }
    var level by remember { mutableStateOf<MenuLevel>(here ?: MenuLevel.Classes) }
    // The list follows the teacher while they are inside it, and starts at their place each time the menu opens.
    LaunchedEffect(screen, data.activeClass) {
        val next = MenuState(level, expanded).followScreen(here)
        level = next.level; expanded = next.expanded
    }
    LaunchedEffect(opening) {
        if (opening) { val next = menuStateOnOpen(here); level = next.level; expanded = next.expanded }
    }
    val today = rememberToday()
    val status = remember(year, today) { year?.let { yearStatus(it, today) }.orEmpty() }
    val hits = remember(year, query) { year?.let { searchGroups(it, query) }.orEmpty() }
    val go: (() -> Unit) -> Unit = { move -> query = ""; onClose(); move() }
    val content: @Composable () -> Unit = {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Space.m, vertical = Space.l),
            verticalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            Row(Modifier.padding(start = Space.s, bottom = Space.s), verticalAlignment = Alignment.CenterVertically) {
                Logo(32.dp)
                Spacer(Modifier.width(Space.m))
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (pin != null) PinButton(pinned, pin)
            }
            if (year != null) SearchField(query, { query = it }, Modifier.padding(bottom = Space.s))
            if (year != null && query.isNotBlank()) {
                if (hits.isEmpty()) Hint(stringResource(R.string.nav_no_match, query.trim()), Modifier.padding(Space.m))
                hits.forEach { h ->
                    DrawerRow(
                        groupLabel(h.className, h.groupName), null, (screen as? Screen.Group)?.id == h.groupId && data.activeClass == h.classId,
                        { go { vm.goToGroup(h.classId, h.groupId) } },
                    ) { StatusLabel(status[h.groupId]) }
                }
            } else {
                Nav.drawer(year != null).forEach { s ->
                    when (s) {
                        Screen.Home -> DrawerRow(stringResource(R.string.nav_home), AppIcons.Home, screen == s, { go { vm.goTo(s) } })
                        Screen.Overview -> {
                            val behind = status.values.count { (it ?: 0) > 0 }
                            val description = pluralStringResource(R.plurals.nav_groups_behind, behind, behind)
                            DrawerRow(stringResource(R.string.app_overview), AppIcons.Overview, screen == s, { go { vm.goTo(s) } }) {
                                if (behind > 0) Tag(pluralStringResource(R.plurals.nav_behind, behind, behind), Modifier.semantics { contentDescription = description }, warn = true)
                            }
                        }
                        Screen.Classes -> if (year != null) DrawerClasses(year, screen, data.activeClass, plansStandIn, status, expanded, { expanded = it }, level, { level = it }, go, vm)
                        Screen.YearSettings -> DrawerRow(
                            year?.name?.ifBlank { null }?.let { stringResource(R.string.nav_year_named, yearShort(it)) } ?: stringResource(R.string.nav_year),
                            AppIcons.Calendar, screen == s, { go { vm.goTo(s) } },
                        )
                        Screen.Settings -> {
                            val description = stringResource(R.string.nav_update_description)
                            DrawerRow(stringResource(R.string.app_settings), AppIcons.Settings, screen == s, { go { vm.goTo(s) } }) {
                                if (updateBadge) Tag(stringResource(R.string.nav_update), Modifier.semantics { contentDescription = description })
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
    // A tablet at 840dp or wider can keep the menu open beside the screens; anywhere else it slides over them.
    if (permanent) PermanentDrawerSheet(Modifier.width(320.dp), drawerContainerColor = c.surface, drawerContentColor = c.ink, drawerTonalElevation = 0.dp) { content() }
    else ModalDrawerSheet(drawerContainerColor = c.surface, drawerContentColor = c.ink, drawerTonalElevation = 0.dp) { content() }
}

/** The pin on a wide tablet's menu: pinned keeps it open beside the screens, unpinned lets it collapse behind ☰. */
@Composable
private fun PinButton(pinned: Boolean, onPin: (Boolean) -> Unit) {
    val c = Pacer.colors
    val haptic = rememberToggleHaptic()
    val label = stringResource(if (pinned) R.string.nav_unpin else R.string.nav_pin)
    IconButton(
        onClick = { haptic(!pinned); onPin(!pinned) },
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (pinned) c.accentSoft else Color.Transparent,
            contentColor = if (pinned) c.onAccentSoft else c.ink,
        ),
        modifier = Modifier.semantics { selected = pinned },
    ) { AppIcon(AppIcons.Pin, contentDescription = label) }
}

/**
 * Classes & Groups in the menu. Collapsed it is one row; open it lists what [menuRows] gives for the [level]: classes,
 * a class's groups, or a group's places. Rows that go deeper only change the list; the rest open a screen and close the menu.
 * Rows under the heading are indented and carry no icon; the five main items do.
 */
@Composable
private fun DrawerClasses(
    year: SchoolYear,
    screen: Screen,
    activeClass: String,
    plansStandIn: Boolean,
    status: Map<String, Int?>,
    expanded: Boolean,
    onExpand: (Boolean) -> Unit,
    level: MenuLevel,
    onLevel: (MenuLevel) -> Unit,
    go: (() -> Unit) -> Unit,
    vm: PacerViewModel,
) {
    // With no classes there is nothing to expand, so the row opens Classes, where the first one is added.
    if (year.classes.isEmpty()) {
        DrawerRow(stringResource(R.string.nav_classes_groups), AppIcons.School, screen == Screen.Classes, { go { vm.goTo(Screen.Classes) } })
        return
    }
    val inside = menuLevelFor(screen, activeClass) != null
    val state = stringResource(if (expanded) R.string.nav_hide_classes else R.string.nav_show_classes)
    DrawerRow(
        stringResource(R.string.nav_classes_groups), AppIcons.School, inside && !expanded, { onExpand(!expanded) },
        Modifier.semantics { stateDescription = state }, markCurrent = false,
    ) { AppIcon(if (expanded) AppIcons.ExpandLess else AppIcons.ExpandMore) }
    if (!expanded) return
    val indent = Modifier.padding(start = Space.xl)
    menuRows(year, level, screen, plansStandIn).forEach { row ->
        when (row) {
            is MenuRow.Up -> {
                val name = row.name?.let { it.ifBlank { stringResource(R.string.app_class) } } ?: stringResource(R.string.app_classes)
                val description = stringResource(R.string.nav_back_to, name)
                DrawerRow(name, AppIcons.Back, false, { onLevel(row.to) }, indent.semantics { contentDescription = description }, accent = true)
            }
            is MenuRow.Heading -> Text(
                row.name.ifBlank { stringResource(if (row.isClass) R.string.app_class else R.string.app_group) },
                style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = indent.padding(horizontal = Space.l, vertical = Space.xs).semantics { heading() },
            )
            is MenuRow.ClassItem -> {
                val description = pluralStringResource(R.plurals.home_groups_count, row.groups, row.groups)
                DrawerRow(row.name.ifBlank { stringResource(R.string.app_class) }, null, false, { onLevel(MenuLevel.Class(row.classId)) }, indent) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${row.groups}", style = MaterialTheme.typography.labelMedium, color = Pacer.colors.muted, modifier = Modifier.semantics { contentDescription = description })
                        AppIcon(AppIcons.ChevronRight)
                    }
                }
            }
            is MenuRow.AllClasses -> DrawerRow(
                pluralStringResource(R.plurals.nav_all_classes, row.count, row.count), null, row.current,
                { go { vm.goTo(Screen.Classes) } }, indent, accent = true,
            )
            is MenuRow.ClassSettings -> DrawerRow(stringResource(R.string.class_class_settings), null, row.current, { go { vm.goToClass(row.classId) } }, indent)
            is MenuRow.GroupItem -> DrawerRow(
                row.name.ifBlank { stringResource(R.string.app_group) }, null, row.current, { go { vm.goToGroup(row.classId, row.groupId) } }, indent,
            ) { StatusLabel(status[row.groupId]) }
            is MenuRow.PlaceItem -> DrawerRow(stringResource(row.place.titleRes()), null, row.current, { go { vm.goTo(row.place.screen(row.groupId)) } }, indent)
        }
    }
}

/**
 * A menu row: an optional symbol, its label and an optional trailing badge, highlighted when it is the open screen.
 * The highlighted row also says "Current page" to TalkBack unless [markCurrent] is false.
 */
@Composable
private fun DrawerRow(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    markCurrent: Boolean = true,
    badge: (@Composable () -> Unit)? = null,
) {
    val c = Pacer.colors
    val current = stringResource(R.string.nav_current_page)
    NavigationDrawerItem(
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (accent && !selected) c.accentText else Color.Unspecified) },
        selected = selected,
        onClick = onClick,
        modifier = if (selected && markCurrent) modifier.semantics { stateDescription = current } else modifier,
        icon = icon?.let { { AppIcon(it) } },
        badge = badge,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = c.accentSoft, selectedTextColor = c.onAccentSoft, selectedIconColor = c.onAccentSoft,
            unselectedTextColor = c.ink, unselectedIconColor = c.ink,
        ),
    )
}

/** One place or group in a list: its name, and at the end its status, or a tick on the one the teacher is on. */
@Composable
private fun GroupRow(label: String, short: Int?, current: Boolean = false, onClick: () -> Unit) {
    val here = stringResource(R.string.nav_current_page)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(vertical = Space.s)
            .semantics { if (current) stateDescription = here },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            color = if (current) Pacer.colors.accentText else Color.Unspecified,
        )
        Spacer(Modifier.width(Space.s))
        if (current) androidx.compose.material3.Icon(AppIcons.Check, contentDescription = null, tint = Pacer.colors.accentText) else StatusLabel(short)
    }
    HorizontalDivider(color = Pacer.colors.line)
}

/**
 * The title switcher (decision 4), one sheet for every group page: the open group's places first, in a fixed order with
 * the current one ticked, then the open class's other groups with their status.
 */
@Composable
fun GroupSwitcherSheet(year: SchoolYear, classId: String, screen: Screen, plansStandIn: Boolean, vm: PacerViewModel, onDismiss: () -> Unit) {
    val groupId = screen.groupId ?: return
    val today = rememberToday()
    val status = remember(year, today) { yearStatus(year, today) }
    val open = year.classes.firstOrNull { it.id == classId }
    val here = open?.groups?.firstOrNull { it.id == groupId }
    HomeSheet(stringResource(R.string.switcher_title), onDismiss) { close ->
        Eyebrow(here?.name?.ifBlank { null } ?: stringResource(R.string.app_group))
        switcherPages(screen, plansStandIn).forEach { p ->
            GroupRow(stringResource(p.place.titleRes()), null, current = p.current) { vm.goTo(p.place.screen(groupId)); close() }
        }
        val others = open?.let { switcherGroups(it.groups, groupId) }.orEmpty()
        if (open != null && others.isNotEmpty()) {
            Eyebrow(open.name.ifBlank { stringResource(R.string.app_class) }, Modifier.padding(top = Space.s))
            others.forEach { g ->
                GroupRow(g.name.ifBlank { stringResource(R.string.app_group) }, status[g.id]) { vm.goToGroup(open.id, g.id); close() }
            }
        }
    }
}

/** ☰, with a small dot in the mark colour (3:1 on every ground, unlike the Saffron fill) while an update is available. TalkBack reads "Menu, update available". */
@Composable
fun MenuButton(onMenu: () -> Unit, badge: Boolean) {
    val c = Pacer.colors
    PillIconButton(stringResource(if (badge) R.string.nav_menu_update else R.string.nav_menu), onMenu) {
        BadgedBox(
            badge = { if (badge) Badge(containerColor = c.mark) },
            modifier = Modifier.clearAndSetSemantics {},
        ) { AppIcon(AppIcons.Menu) }
    }
}

/** The warning button in a group's top bar: a symbol with the count on a badge, read as "2 warnings". */
@Composable
fun WarningButton(count: Int, onClick: () -> Unit) {
    val c = Pacer.colors
    PillIconButton(pluralStringResource(R.plurals.warnings_count, count, count), onClick) {
        BadgedBox(
            badge = { Badge(containerColor = c.warn, contentColor = c.onWarn) { Text("$count") } },
            modifier = Modifier.clearAndSetSemantics {},
        ) { AppIcon(AppIcons.Warning) }
    }
}

/** What each fix in the Warnings sheet does. Catch up and Bring in step open their own sheets. */
class WarningFixes(
    val catchUp: () -> Unit,
    val fold: (BookLag) -> Unit,
    val repace: () -> Unit,
    val fit: () -> Unit,
    val tidy: () -> Unit,
    val extra: (java.time.LocalDate) -> Unit = {},
    val repaceToFit: () -> Unit = repace,
    val units: (gr.edusuite.pacer.engine.UnitIssue) -> Unit = {},
    val openLesson: (String) -> Unit = {},
)

/**
 * The Warnings sheet (decision 9): every warning on the group with its fix. Catch up and Bring in step close it and
 * open their sheets; the custom plan's fixes close it too so their Undo notification is visible.
 */
@Composable
fun WarningsSheet(report: GroupReport, warnings: List<GroupWarning>, fixes: WarningFixes, onDismiss: () -> Unit) {
    val plan = report.plan
    val title = groupLabel(plan.name, report.group.name)
    HomeSheet(stringResource(R.string.warnings_title, title), onDismiss) { close ->
        if (warnings.isEmpty()) Hint(stringResource(R.string.warnings_none))
        warnings.forEachIndexed { i, w ->
            if (i > 0) HorizontalDivider(color = Pacer.colors.line)
            when (w) {
                is GroupWarning.Units -> WarningItem(AppIcons.Book, stringResource(R.string.units_title),
                    w.issue.message, stringResource(if (w.issue.kind == gr.edusuite.pacer.engine.UnitIssueKind.REVISION_UNSCHEDULED) R.string.class_edit else R.string.units_title)) {
                    close(); fixes.units(w.issue)
                }
                is GroupWarning.ExtraUnavailable -> WarningItem(
                    AppIcons.Warning, stringResource(R.string.extra_unavailable, w.date.fmt()),
                    stringResource(R.string.extra_unavailable_hint), stringResource(R.string.extra_edit),
                ) { close(); fixes.extra(w.date) }
                is GroupWarning.Behind -> Column {
                    WarningItem(
                        AppIcons.Warning, pluralStringResource(R.plurals.status_short, w.lessons, w.lessons),
                        pluralStringResource(R.plurals.warn_behind, w.pages, w.pages, plan.pace.plain()),
                        stringResource(R.string.warn_catch_up), strong = true,
                    ) { close(); fixes.catchUp() }
                    if (report.custom != null) TertiaryButton(
                        stringResource(R.string.warnings_repace_to_fit), { close(); fixes.repaceToFit() },
                        Modifier.fillMaxWidth(), color = Pacer.colors.accentText,
                    )
                }
                is GroupWarning.BookBehind -> WarningItem(
                    AppIcons.Book,
                    pluralStringResource(R.plurals.warnings_book_behind, w.lessons, plan.books.firstOrNull { it.id == w.lag.bookId }?.name.orEmpty(), w.lessons),
                    stringResource(R.string.warnings_book_hint), stringResource(R.string.warn_fold),
                ) { close(); fixes.fold(w.lag) }
                is GroupWarning.Missing -> WarningItem(
                    AppIcons.Warning, pluralStringResource(R.plurals.custom_missing, w.pages, w.pages),
                    stringResource(R.string.warnings_plan_hint), stringResource(R.string.custom_fix_repace), onFix = { close(); fixes.repace() },
                )
                is GroupWarning.Overflow -> WarningItem(
                    AppIcons.Warning, pluralStringResource(R.plurals.custom_overflow, w.lessons, w.lessons),
                    overflowHint(w), if (w.canFit) stringResource(R.string.custom_fix_fit) else null, onFix = { close(); fixes.fit() },
                )
                is GroupWarning.Doubled -> WarningItem(
                    AppIcons.Warning, pluralStringResource(R.plurals.custom_doubled, w.pages, w.pages),
                    if (w.openLesson == null) stringResource(R.string.warnings_plan_hint) else stringResource(R.string.warnings_edited_hint),
                    stringResource(if (w.openLesson == null) R.string.custom_fix_tidy else R.string.custom_fix_open_lesson),
                    onFix = { close(); w.openLesson?.let(fixes.openLesson) ?: fixes.tidy() },
                )
                is GroupWarning.Stale -> WarningItem(
                    AppIcons.Warning, pluralStringResource(R.plurals.custom_stale, w.pages, w.pages),
                    if (w.openLesson == null) stringResource(R.string.warnings_plan_hint) else stringResource(R.string.warnings_edited_hint),
                    stringResource(if (w.openLesson == null) R.string.custom_fix_tidy else R.string.custom_fix_open_lesson),
                    onFix = { close(); w.openLesson?.let(fixes.openLesson) ?: fixes.tidy() },
                )
            }
        }
    }
}

/** Why the Overflow warning stays: the lessons the teacher edited stay where they are, and Fit to term may have nowhere to put the rest. */
@Composable
private fun overflowHint(w: GroupWarning.Overflow): String {
    val base = stringResource(R.string.warnings_plan_hint)
    val edited = if (w.edited > 0) pluralStringResource(R.plurals.warnings_overflow_edited, w.edited, w.edited) else null
    val stuck = if (!w.canFit && w.edited < w.lessons) stringResource(R.string.warnings_overflow_stuck) else null
    return listOfNotNull(base, edited, stuck).joinToString(" ")
}

/** A warning: its symbol, what is wrong over a line on why, and the button that fixes it, if any ([strong] fills it in warn). */
@Composable
private fun WarningItem(icon: ImageVector, title: String, hint: String, fix: String?, strong: Boolean = false, onFix: () -> Unit = {}) {
    val c = Pacer.colors
    Row(Modifier.fillMaxWidth().padding(vertical = Space.s), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
        AppIcon(icon, Modifier.padding(top = Space.xs))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(hint, style = MaterialTheme.typography.bodyMedium, color = c.muted)
            if (fix == null) Unit else if (strong) {
                Button(
                    onClick = onFix, modifier = Modifier.heightIn(min = 48.dp), shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = c.warn, contentColor = c.onWarn),
                ) { Text(fix, style = MaterialTheme.typography.labelLarge) }
            } else {
                SecondaryButton(fix, onFix)
            }
        }
    }
}
