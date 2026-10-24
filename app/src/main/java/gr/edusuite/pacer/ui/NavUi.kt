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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
 * The ☰ menu (decision 3): search over the year's classes and groups, then Home, Pacing overview with how many groups
 * are behind, Classes as an expandable list, School year and Settings. [onClose] closes the drawer before it moves.
 * With [permanent] (a tablet 840dp wide or more) it is the always-open drawer beside the screens.
 */
@Composable
fun AppDrawer(data: AppData, screen: Screen, vm: PacerViewModel, onClose: () -> Unit, permanent: Boolean = false) {
    val c = Pacer.colors
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val year = data.year
    val status = remember(year) { year?.let(::yearStatus).orEmpty() }
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
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
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
                        Screen.Classes -> if (year != null) DrawerClasses(year, screen, data.activeClass, expanded, { expanded = it }, go, vm)
                        Screen.YearSettings -> DrawerRow(
                            year?.name?.ifBlank { null }?.let { stringResource(R.string.nav_year_named, it) } ?: stringResource(R.string.nav_year),
                            AppIcons.Calendar, screen == s, { go { vm.goTo(s) } },
                        )
                        Screen.Settings -> DrawerRow(stringResource(R.string.app_settings), AppIcons.Settings, screen == s, { go { vm.goTo(s) } })
                        else -> Unit
                    }
                }
            }
        }
    }
    // A tablet at 840dp or wider keeps the menu open beside the screens; anywhere else it slides over them.
    if (permanent) PermanentDrawerSheet(Modifier.width(320.dp), drawerContainerColor = c.surface, drawerContentColor = c.ink, drawerTonalElevation = 0.dp) { content() }
    else ModalDrawerSheet(drawerContainerColor = c.surface, drawerContentColor = c.ink, drawerTonalElevation = 0.dp) { content() }
}

/** Classes in the menu: the row opens and closes the list of classes, each with its group count, and "All 9 classes". */
@Composable
private fun DrawerClasses(
    year: SchoolYear,
    screen: Screen,
    activeClass: String,
    expanded: Boolean,
    onExpand: (Boolean) -> Unit,
    go: (() -> Unit) -> Unit,
    vm: PacerViewModel,
) {
    // With no classes there is nothing to expand, so the row opens Classes, where the first one is added.
    if (year.classes.isEmpty()) {
        DrawerRow(stringResource(R.string.app_classes), AppIcons.School, screen == Screen.Classes, { go { vm.goTo(Screen.Classes) } })
        return
    }
    val state =stringResource(if (expanded) R.string.nav_hide_classes else R.string.nav_show_classes)
    DrawerRow(
        stringResource(R.string.app_classes), AppIcons.School, screen == Screen.Classes, { onExpand(!expanded) },
        Modifier.semantics { stateDescription = state },
    ) { AppIcon(if (expanded) AppIcons.ExpandLess else AppIcons.ExpandMore) }
    if (!expanded) return
    year.classes.forEach { cl ->
        val count = cl.groups.size
        val description = pluralStringResource(R.plurals.home_groups_count, count, count)
        DrawerRow(
            cl.name.ifBlank { stringResource(R.string.app_class) }, null, screen == Screen.Class && activeClass == cl.id,
            { go { vm.goToClass(cl.id) } }, Modifier.padding(start = Space.xl),
        ) {
            Text("$count", style = MaterialTheme.typography.labelMedium, color = Pacer.colors.muted, modifier = Modifier.semantics { contentDescription = description })
        }
    }
    DrawerRow(
        pluralStringResource(R.plurals.nav_all_classes, year.classes.size, year.classes.size), null, false,
        { go { vm.goTo(Screen.Classes) } }, Modifier.padding(start = Space.xl), accent = true,
    )
}

/** A menu row: an optional symbol, its label and an optional trailing badge, highlighted when it is the open screen. */
@Composable
private fun DrawerRow(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    badge: (@Composable () -> Unit)? = null,
) {
    val c = Pacer.colors
    NavigationDrawerItem(
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (accent && !selected) c.accentText else Color.Unspecified) },
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        icon = icon?.let { { AppIcon(it) } },
        badge = badge,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = c.accentSoft, selectedTextColor = c.onAccentSoft, selectedIconColor = c.onAccentSoft,
            unselectedTextColor = c.ink, unselectedIconColor = c.ink,
        ),
    )
}

/** One group in a list: its name, "you are here" on the open one, and its status at the end. */
@Composable
private fun GroupRow(label: String, short: Int?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(Space.s))
        StatusLabel(short)
    }
    HorizontalDivider(color = Pacer.colors.line)
}

/**
 * The group switcher (decision 4), opened from the group's title: search, the groups opened recently this session
 * and the open class's groups, each with its status.
 */
@Composable
fun GroupSwitcherSheet(year: SchoolYear, classId: String, groupId: String, recent: List<GroupRef>, vm: PacerViewModel, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val status = remember(year) { yearStatus(year) }
    val hits = remember(year, query) { searchGroups(year, query) }
    // Recent groups that still exist, other than the open one.
    val recents = remember(year, recent, groupId) {
        recent.filter { it.groupId != groupId }.mapNotNull { r ->
            val cl = year.classes.firstOrNull { it.id == r.classId } ?: return@mapNotNull null
            val g = cl.groups.firstOrNull { it.id == r.groupId } ?: return@mapNotNull null
            GroupHit(cl.id, cl.name, g.id, g.name)
        }.take(Nav.RECENT - 1)
    }
    val open = year.classes.firstOrNull { it.id == classId }
    HomeSheet(stringResource(R.string.switcher_title), onDismiss) { close ->
        val go = { h: GroupHit -> vm.goToGroup(h.classId, h.groupId); close() }
        SearchField(query, { query = it })
        if (query.isNotBlank()) {
            if (hits.isEmpty()) Hint(stringResource(R.string.nav_no_match, query.trim()))
            hits.forEach { h -> GroupRow(groupLabel(h.className, h.groupName), status[h.groupId]) { go(h) } }
            return@HomeSheet
        }
        if (recents.isNotEmpty()) {
            Eyebrow(stringResource(R.string.switcher_recent), Modifier.padding(top = Space.s))
            recents.forEach { h -> GroupRow(groupLabel(h.className, h.groupName), status[h.groupId]) { go(h) } }
        }
        if (open != null) {
            Eyebrow(open.name.ifBlank { stringResource(R.string.app_class) }, Modifier.padding(top = Space.s))
            open.groups.forEach { g ->
                val name = g.name.ifBlank { stringResource(R.string.app_group) }
                GroupRow(if (g.id == groupId) stringResource(R.string.switcher_here, name) else name, status[g.id]) {
                    if (g.id == groupId) close() else go(GroupHit(open.id, open.name, g.id, g.name))
                }
            }
        }
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
                    stringResource(R.string.warnings_plan_hint), stringResource(R.string.custom_fix_fit), onFix = { close(); fixes.fit() },
                )
                is GroupWarning.Doubled -> WarningItem(
                    AppIcons.Warning, pluralStringResource(R.plurals.custom_doubled, w.pages, w.pages),
                    stringResource(R.string.warnings_plan_hint), stringResource(R.string.custom_fix_tidy), onFix = { close(); fixes.tidy() },
                )
                is GroupWarning.Stale -> WarningItem(
                    AppIcons.Warning, pluralStringResource(R.plurals.custom_stale, w.pages, w.pages),
                    stringResource(R.string.warnings_plan_hint), stringResource(R.string.custom_fix_tidy), onFix = { close(); fixes.tidy() },
                )
            }
        }
    }
}

/** A warning: its symbol, what is wrong over a line on why, and the button that fixes it ([strong] fills it in warn). */
@Composable
private fun WarningItem(icon: ImageVector, title: String, hint: String, fix: String, strong: Boolean = false, onFix: () -> Unit) {
    val c = Pacer.colors
    Row(Modifier.fillMaxWidth().padding(vertical = Space.s), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
        AppIcon(icon, Modifier.padding(top = Space.xs))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(hint, style = MaterialTheme.typography.bodyMedium, color = c.muted)
            if (strong) {
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
