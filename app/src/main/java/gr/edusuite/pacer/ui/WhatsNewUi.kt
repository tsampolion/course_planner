package gr.edusuite.pacer.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import gr.edusuite.pacer.R
import gr.edusuite.pacer.model.Module
import gr.edusuite.pacer.ui.theme.Figures
import gr.edusuite.pacer.ui.theme.Motion
import gr.edusuite.pacer.ui.theme.MotionSpec
import gr.edusuite.pacer.ui.theme.Pacer
import gr.edusuite.pacer.ui.theme.Space
import kotlinx.coroutines.launch

/**
 * What's new: a short deck of cards shown once after an update. There is no Skip and no close: every card has Continue
 * (the last one finishes), Back steps to the previous card, and swiping works too. System Back also steps back and
 * never closes the deck. The last card can open Features without leaving the deck.
 */
@Composable
fun WhatsNewDeck(name: String, releaseLabel: String, onChooseTools: () -> Unit, onFinish: () -> Unit) {
    val cards = remember { deckCards() }
    val pager = rememberPagerState { cards.size }
    val scope = rememberCoroutineScope()
    val motion = animationsEnabled()
    fun goTo(index: Int) {
        scope.launch { if (motion) pager.animateScrollToPage(index, animationSpec = Motion.work(MotionSpec.Step.SHEET)) else pager.scrollToPage(index) }
    }
    val c = Pacer.colors
    Dialog(
        onDismissRequest = { goTo(deckBack(pager.currentPage)) },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = c.paper, contentColor = c.ink) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(Space.l), horizontalAlignment = Alignment.CenterHorizontally) {
                Column(Modifier.widthIn(max = 560.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(releaseLabel, style = MaterialTheme.typography.labelMedium, color = c.muted)
                        Text(
                            stringResource(R.string.deck_position, pager.currentPage + 1, cards.size),
                            style = MaterialTheme.typography.labelMedium.merge(Figures), color = c.muted,
                        )
                    }
                    HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), pageSpacing = Space.m) { page ->
                        DeckCardView(cards[page], name, onChooseTools)
                    }
                    Dots(cards.size, pager.currentPage, Modifier.align(Alignment.CenterHorizontally))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        if (pager.currentPage > 0) SecondaryButton(stringResource(R.string.deck_back), { goTo(deckBack(pager.currentPage)) })
                        PrimaryButton(
                            stringResource(R.string.deck_continue),
                            { deckNext(pager.currentPage)?.let(::goTo) ?: onFinish() },
                            Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Dots(count: Int, current: Int, modifier: Modifier = Modifier) {
    val c = Pacer.colors
    val motion = animationsEnabled()
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val width by animateDpAsState(if (i == current) 20.dp else 6.dp, if (motion) Motion.work(MotionSpec.Step.PRESS) else Motion.work(MotionSpec.Step.PRESS, false))
            Box(Modifier.height(6.dp).width(width).clip(CircleShape).background(if (i == current) c.accent else c.line))
        }
    }
}

@Composable
private fun DeckCardView(card: DeckCard, name: String, onChooseTools: () -> Unit) {
    val c = Pacer.colors
    Surface(
        Modifier.fillMaxSize(), shape = MaterialTheme.shapes.large, color = c.surface,
        border = BorderStroke(1.dp, c.line),
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(Space.xl), verticalArrangement = Arrangement.spacedBy(Space.m)) {
            when (card) {
                DeckCard.HELLO -> {
                    val greeting = deckName(name)?.let { stringResource(R.string.deck_greeting_named, it) } ?: stringResource(R.string.deck_greeting)
                    Text(stringResource(R.string.deck_hello, greeting), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                    Text(stringResource(R.string.deck_hello_body), style = MaterialTheme.typography.bodyLarge, color = c.muted)
                }
                DeckCard.CHOOSE -> {
                    Text(stringResource(R.string.deck_choose_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                    Text(stringResource(R.string.deck_choose_body), style = MaterialTheme.typography.bodyLarge, color = c.muted)
                    SecondaryButton(stringResource(R.string.deck_choose_button), onChooseTools, Modifier.fillMaxWidth())
                }
                else -> {
                    val module = card.module!!
                    Text(stringResource(module.titleRes()), style = MaterialTheme.typography.labelLarge, color = c.muted)
                    Text(stringResource(card.titleRes()), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                    Text(stringResource(card.bodyRes()), style = MaterialTheme.typography.bodyLarge, color = c.muted)
                    Fragment(module)
                }
            }
        }
    }
}

private fun DeckCard.titleRes(): Int = when (this) {
    DeckCard.WORK -> R.string.deck_work_title
    DeckCard.BOOK_TOOLS -> R.string.deck_book_tools_title
    DeckCard.PLANNING -> R.string.deck_planning_title
    DeckCard.OUTPUT -> R.string.deck_output_title
    DeckCard.HELLO, DeckCard.CHOOSE -> R.string.deck_choose_title
}

private fun DeckCard.bodyRes(): Int = when (this) {
    DeckCard.WORK -> R.string.deck_work_body
    DeckCard.BOOK_TOOLS -> R.string.deck_book_tools_body
    DeckCard.PLANNING -> R.string.deck_planning_body
    DeckCard.OUTPUT -> R.string.deck_output_body
    DeckCard.HELLO, DeckCard.CHOOSE -> R.string.deck_choose_body
}

/** A real piece of the screen the tool adds: ledger rows with the kind edge and tint, or the ⋮ menu's items. */
@Composable
private fun Fragment(module: Module) {
    val c = Pacer.colors
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = c.paper, border = BorderStroke(1.dp, c.line)) {
        Column(Modifier.padding(horizontal = Space.l, vertical = Space.xs)) {
            when (module) {
                Module.WORK -> {
                    FragmentRow(R.string.deck_frag_work_1, R.string.deck_frag_work_1d, c.edgeHand)
                    HorizontalDivider(color = c.line)
                    FragmentRow(R.string.deck_frag_work_2, R.string.deck_frag_work_2d, c.edgeTest)
                }
                Module.BOOK_TOOLS -> {
                    FragmentRow(R.string.deck_frag_book_1, R.string.deck_frag_book_1d, c.edgeEdited)
                    HorizontalDivider(color = c.line)
                    FragmentRow(R.string.deck_frag_book_2, R.string.deck_frag_book_2d, c.edgeSupp)
                }
                Module.PLANNING -> listOf(R.string.deck_frag_plan_1, R.string.deck_frag_plan_2, R.string.deck_frag_plan_3).forEachIndexed { i, r ->
                    if (i > 0) HorizontalDivider(color = c.line)
                    MenuLine(r)
                }
                Module.OUTPUT -> listOf(R.string.deck_frag_out_1, R.string.deck_frag_out_2, R.string.deck_frag_out_3).forEachIndexed { i, r ->
                    if (i > 0) HorizontalDivider(color = c.line)
                    MenuLine(r)
                }
            }
        }
    }
}

@Composable
private fun FragmentRow(title: Int, detail: Int, edge: androidx.compose.ui.graphics.Color) {
    val c = Pacer.colors
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(c.kindTint(edge)).padding(vertical = Space.m), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(edge, MaterialTheme.shapes.extraSmall))
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(detail), style = MaterialTheme.typography.bodyMedium.merge(Figures), color = c.muted)
        }
    }
}

@Composable
private fun MenuLine(label: Int) {
    Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().padding(vertical = Space.m))
}

/** The deck shown once after an update, on an install that already had data (a fresh install sees Welcome instead). */
@Composable
fun UpdateDeck(settings: gr.edusuite.pacer.model.Settings, hasYears: Boolean, vm: PacerViewModel) {
    val version = rememberVersionName()
    val id = deckId(version)
    var choosing by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    if (deckDue(settings.seenTips, id, existingInstall = settings.welcomeSeen || hasYears)) {
        WhatsNewDeck(settings.name, releaseLabel(version, stringResource(R.string.release_name)), onChooseTools = { choosing = true }, onFinish = { vm.markHintSeen(id) })
    }
    if (choosing) FeaturesSheet(LocalFeatures.current, { vm.turnOn(it) }, { choosing = false })
}
