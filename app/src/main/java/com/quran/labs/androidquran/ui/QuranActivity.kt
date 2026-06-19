package com.quran.labs.androidquran.ui

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.Builder
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.quran.data.dao.RecentPagesDao
import com.quran.labs.androidquran.AboutUsActivity
import com.quran.labs.androidquran.HelpActivity
import com.quran.labs.androidquran.QuranApplication
import com.quran.labs.androidquran.QuranPreferenceActivity
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.SearchActivity
import com.quran.labs.androidquran.ShortcutsActivity
import com.quran.labs.androidquran.data.Constants
import com.quran.labs.androidquran.feature.reading.model.LatestPageTracker
import com.quran.labs.androidquran.presenter.data.QuranIndexEventLogger
import com.quran.labs.androidquran.presenter.translation.TranslationManagerPresenter
import com.quran.labs.androidquran.service.AudioService
import com.quran.labs.androidquran.ui.fragment.AddTagDialog
import com.quran.labs.androidquran.ui.fragment.AddTagDialog.Companion.newInstance
import com.quran.labs.androidquran.ui.fragment.BookmarksFragment
import com.quran.labs.androidquran.ui.fragment.JumpFragment
import com.quran.labs.androidquran.ui.fragment.KomunitasFragment
import com.quran.labs.androidquran.ui.fragment.PendampingIbadahFragment
import com.quran.labs.androidquran.ui.fragment.ProfilFragment
import com.quran.labs.androidquran.ui.fragment.TagBookmarkDialog
import com.quran.labs.androidquran.ui.fragment.TagBookmarkDialog.OnBookmarkTagsUpdateListener
import com.quran.labs.androidquran.ui.fragment.EventFragment
import com.quran.labs.androidquran.ui.fragment.TilawahFragment
import com.quran.labs.androidquran.ui.helpers.JumpDestination
import com.quran.labs.androidquran.util.AudioUtils
import com.quran.labs.androidquran.util.QuranSettings
import com.quran.labs.androidquran.util.QuranUtils
import com.quran.mobile.di.ExtraScreenProvider
import dev.zacsweers.metro.Inject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * The home screen activity for the app. Displays a toolbar and 4 bottom navigation tabs:
 *
 *  * Pendamping Ibadah (Worship Companion)
 *  * Komunitas (Community)
 *  * Tilawah (Recitation) — contains the original SuraList, JuzList, Bookmarks pages
 *  * Profil (Profile)
 *
 * When this activity is created, it may run a background check to see if updated translations
 * are available, and if so, show a dialog asking the user if they want to download them.
 *
 * This activity is called from several places:
 *  * [com.quran.labs.androidquran.QuranDataActivity]
 *  * [ShortcutsActivity]
 */
class QuranActivity : AppCompatActivity(),
    OnBookmarkTagsUpdateListener,
    JumpDestination {
  private var upgradeDialog: AlertDialog? = null
  private var showedTranslationUpgradeDialog = false
  private var isRtl = false
  private var isPaused = false
  private var searchItem: MenuItem? = null
  private var supportActionMode: ActionMode? = null
  private val compositeDisposable = CompositeDisposable()
  private val latestPageFlow: Flow<Int> by lazy {
    combine(
      recentPagesDao.recentPagesFlow()
        .map { recentPages -> recentPages.firstOrNull()?.page ?: Constants.NO_PAGE },
      latestPageTracker.latestPage
    ) { persistedPage, latestPage ->
      latestPage
        ?.takeIf { it.pageType == settings.pageType }
        ?.page
        ?: persistedPage
    }
      .distinctUntilChanged()
  }

  suspend fun latestPage(): Int {
    return latestPageFlow.first()
  }

  private var backStackListener: FragmentManager.OnBackStackChangedListener? = null
  private lateinit var searchItemCollapserCallback: OnBackPressedCallback
  private lateinit var supportActionModeClearingCallback: OnBackPressedCallback

  @Inject
  lateinit var settings: QuranSettings
  @Inject
  lateinit var audioUtils: AudioUtils
  @Inject
  lateinit var recentPagesDao: RecentPagesDao
  @Inject
  lateinit var latestPageTracker: LatestPageTracker
  @Inject
  lateinit var translationManagerPresenter: TranslationManagerPresenter
  @Inject
  lateinit var quranIndexEventLogger: QuranIndexEventLogger
  @Inject
  lateinit var extraScreens: Set<@JvmSuppressWildcards ExtraScreenProvider>

  public override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    val quranApp = application as QuranApplication
    quranApp.applicationComponent
      .activityComponentFactory()
      .generate(this)
      .quranActivityComponentFactory()
      .generate()
      .inject(this)

    registerBackPressedCallbacks()
    setContentView(R.layout.quran_index)
    isRtl = isRtl()

    val root = findViewById<ViewGroup>(R.id.root)
    ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
      val insets = windowInsets.getInsets(
        WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
      )
      root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        topMargin = insets.top
        leftMargin = insets.left
        rightMargin = insets.right
      }
      windowInsets
    }

    val tb = findViewById<Toolbar>(R.id.toolbar)
    setSupportActionBar(tb)
    val ab = supportActionBar
    ab?.setTitle(R.string.app_name)

    // Set up BottomNavigationView
    val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

    // Show Home tab by default
    if (savedInstanceState == null) {
      val defaultItemId = R.id.navigation_pendamping_ibadah
      bottomNav.selectedItemId = defaultItemId
      switchFragment(PendampingIbadahFragment(), TAG_PENDAMPING)
    } else {
      // Restore the selected tab state from savedInstanceState
      val selectedTag = savedInstanceState.getString(KEY_SELECTED_NAV_TAG, TAG_PENDAMPING)
      val selectedId = savedInstanceState.getInt(KEY_SELECTED_NAV_ID, R.id.navigation_pendamping_ibadah)
      bottomNav.selectedItemId = selectedId
      val existingFragment = supportFragmentManager.findFragmentByTag(selectedTag)
      if (existingFragment == null) {
        switchFragment(fragmentForTag(selectedTag), selectedTag)
      }
    }

    bottomNav.setOnItemSelectedListener { item ->
      when (item.itemId) {
        R.id.navigation_pendamping_ibadah -> switchFragment(PendampingIbadahFragment(), TAG_PENDAMPING)
        R.id.navigation_komunitas        -> switchFragment(KomunitasFragment(), TAG_KOMUNITAS)
        R.id.navigation_event            -> switchFragment(EventFragment(), TAG_EVENT)
        R.id.navigation_tilawah          -> switchFragment(TilawahFragment(), TAG_TILAWAH)
        R.id.navigation_profil           -> switchFragment(ProfilFragment(), TAG_PROFIL)
      }
      true
    }

    findViewById<View>(R.id.btn_center_event)?.setOnClickListener {
      bottomNav.selectedItemId = R.id.navigation_event
    }

    if (savedInstanceState != null) {
      showedTranslationUpgradeDialog = savedInstanceState.getBoolean(
          SI_SHOWED_UPGRADE_DIALOG, false
      )
    }

    val intent = intent
    if (intent != null) {
      val extras = intent.extras
      if (extras != null) {
        if (extras.getBoolean(EXTRA_SHOW_TRANSLATION_UPGRADE, false)) {
          if (!showedTranslationUpgradeDialog) {
            showTranslationsUpgradeDialog()
          }
        }
      }
      if (ShortcutsActivity.ACTION_JUMP_TO_LATEST == intent.action) {
        jumpToLastPage()
      }
    }
    updateTranslationsListAsNeeded()
    quranIndexEventLogger.logAnalytics()
  }

  private fun switchFragment(fragment: Fragment, tag: String) {
    val existingFragment = supportFragmentManager.findFragmentByTag(tag)
    supportFragmentManager.beginTransaction()
      .replace(R.id.fragment_container, existingFragment ?: fragment, tag)
      .commitAllowingStateLoss()
  }

  private fun fragmentForTag(tag: String): Fragment {
    return when (tag) {
      TAG_PENDAMPING -> PendampingIbadahFragment()
      TAG_KOMUNITAS  -> KomunitasFragment()
      TAG_EVENT      -> EventFragment()
      TAG_TILAWAH    -> TilawahFragment()
      TAG_PROFIL     -> ProfilFragment()
      else           -> PendampingIbadahFragment()
    }
  }

  public override fun onResume() {
    super.onResume()
    val isRtl = isRtl()
    if (isRtl != this.isRtl) {
      val i = intent
      finish()
      startActivity(i)
    }

    compositeDisposable.add(
        Completable.timer(500, MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
              try {
                startService(
                  audioUtils.getAudioIntent(this@QuranActivity, AudioService.ACTION_STOP)
                )
              } catch (_: IllegalStateException) {
                // do nothing, we might be in the background
              }
            }
    )
    isPaused = false
  }

  override fun onPause() {
    compositeDisposable.clear()
    isPaused = true
    super.onPause()
  }

  override fun onDestroy() {
    backStackListener?.let {
      supportFragmentManager.removeOnBackStackChangedListener(it)
    }
    super.onDestroy()
  }

  // on back pressed, these are run in reverse order of registration
  private fun registerBackPressedCallbacks() {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && isTaskRoot) {
      val enabled = (supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.backStackEntryCount ?: 0) == 0 &&
          supportFragmentManager.backStackEntryCount == 0
      val callback = object : OnBackPressedCallback(enabled) {
        override fun handleOnBackPressed() {
          finishAfterTransition()
        }
      }
      onBackPressedDispatcher.addCallback(this, callback)

      val listener = FragmentManager.OnBackStackChangedListener {
        callback.isEnabled =
          (supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.backStackEntryCount
            ?: 0) == 0 &&
              supportFragmentManager.backStackEntryCount == 0
      }
      backStackListener = listener
      supportFragmentManager.addOnBackStackChangedListener(listener)
    }

    val searchItemExpanded = searchItem?.isActionViewExpanded ?: false
    searchItemCollapserCallback = object : OnBackPressedCallback(searchItemExpanded) {
      override fun handleOnBackPressed() {
        val searchItem = searchItem
        if (searchItem != null && searchItem.isActionViewExpanded) {
          searchItem.collapseActionView()
        }
        isEnabled = false
      }
    }

    val supportActionModeEnabled = supportActionMode != null
    supportActionModeClearingCallback = object : OnBackPressedCallback(supportActionModeEnabled) {
      override fun handleOnBackPressed() {
        supportActionMode?.finish()
      }
    }
  }

  private fun isRtl(): Boolean {
    return QuranUtils.isRtl()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    super.onCreateOptionsMenu(menu)
    val inflater = menuInflater
    inflater.inflate(R.menu.home_menu, menu)
    searchItem = menu.findItem(R.id.search)
    searchItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
      override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        searchItemCollapserCallback.isEnabled = true
        return true
      }

      override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        searchItemCollapserCallback.isEnabled = false
        return true
      }
    })
    val searchView = searchItem?.actionView as SearchView
    val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
    searchView.queryHint = getString(R.string.search_hint)
    searchView.setSearchableInfo(
        searchManager.getSearchableInfo(
            ComponentName(this, SearchActivity::class.java)
        )
    )

    extraScreens
      .sortedBy { it.order }
      .forEach { menu.add(Menu.NONE, it.id, Menu.NONE, it.titleResId) }

    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (val itemId = item.itemId) {
      R.id.settings -> {
        startActivity(Intent(this, QuranPreferenceActivity::class.java))
      }
      R.id.last_page -> {
        jumpToLastPage()
      }
      R.id.help -> {
        startActivity(Intent(this, HelpActivity::class.java))
      }
      R.id.about -> {
        startActivity(Intent(this, AboutUsActivity::class.java))
      }
      R.id.jump -> {
        gotoPageDialog()
      }
      R.id.other_apps -> {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = "market://search?q=pub:quran.com".toUri()
        if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) == null) {
          intent.data = "https://play.google.com/store/search?q=pub:quran.com".toUri()
        }
        startActivity(intent)
      }
      else -> {
        val handled = extraScreens.firstOrNull { it.id == itemId }?.onClick(this) ?: false
        return handled || super.onOptionsItemSelected(item)
      }
    }
    return true
  }

  override fun onSupportActionModeFinished(mode: ActionMode) {
    supportActionMode = null
    supportActionModeClearingCallback.isEnabled = false
    super.onSupportActionModeFinished(mode)
  }

  override fun onSupportActionModeStarted(mode: ActionMode) {
    supportActionMode = mode
    supportActionModeClearingCallback.isEnabled = true
    super.onSupportActionModeStarted(mode)

    val abRoot = findViewById<ViewGroup>(androidx.appcompat.R.id.action_bar_root)
    abRoot.post {
      val statusGuard = abRoot.getChildAt(abRoot.childCount - 1)
      statusGuard?.let {
        if (statusGuard::class == View::class && statusGuard.top == 0) {
          statusGuard.setBackgroundColor(ContextCompat.getColor(this, R.color.toolbar))
        }
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putBoolean(SI_SHOWED_UPGRADE_DIALOG, showedTranslationUpgradeDialog)
    val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
    outState.putInt(KEY_SELECTED_NAV_ID, bottomNav.selectedItemId)
    val tag = when (bottomNav.selectedItemId) {
      R.id.navigation_pendamping_ibadah -> TAG_PENDAMPING
      R.id.navigation_komunitas        -> TAG_KOMUNITAS
      R.id.navigation_event            -> TAG_EVENT
      R.id.navigation_tilawah          -> TAG_TILAWAH
      R.id.navigation_profil           -> TAG_PROFIL
      else                             -> TAG_PENDAMPING
    }
    outState.putString(KEY_SELECTED_NAV_TAG, tag)
    super.onSaveInstanceState(outState)
  }

  private fun jumpToLastPage() {
    lifecycleScope.launch {
      val recentPage = latestPage()
      jumpTo(
        if (recentPage == Constants.NO_PAGE) 1 else recentPage
      )
    }
  }

  private fun updateTranslationsListAsNeeded() {
    if (!updatedTranslations) {
      translationManagerPresenter.checkForUpdates()
      updatedTranslations = true
    }
  }

  private fun showTranslationsUpgradeDialog() {
    showedTranslationUpgradeDialog = true

    val builder = Builder(this)
    builder.setMessage(R.string.translation_updates_available)
    builder.setCancelable(false)
    builder.setPositiveButton(R.string.translation_dialog_yes) { dialog: DialogInterface, _: Int ->
      dialog.dismiss()
      upgradeDialog = null
      launchTranslationActivity()
    }

    builder.setNegativeButton(R.string.translation_dialog_later) { dialog: DialogInterface, _: Int ->
      dialog.dismiss()
      upgradeDialog = null
      settings.setHaveUpdatedTranslations(false)
    }

    val dialog = builder.create()
    dialog.show()
    upgradeDialog = dialog
  }

  private fun launchTranslationActivity() {
    val i = Intent(this, TranslationManagerActivity::class.java)
    startActivity(i)
  }

  override fun jumpTo(page: Int) {
    val i = Intent(this, PagerActivity::class.java)
    i.putExtra("page", page)
    i.putExtra(PagerActivity.EXTRA_JUMP_TO_TRANSLATION, settings.wasShowingTranslation)
    startActivity(i)
  }

  override fun jumpToAndHighlight(page: Int, sura: Int, ayah: Int) {
    val i = Intent(this, PagerActivity::class.java)
    i.putExtra("page", page)
    i.putExtra(PagerActivity.EXTRA_HIGHLIGHT_SURA, sura)
    i.putExtra(PagerActivity.EXTRA_HIGHLIGHT_AYAH, ayah)
    i.putExtra(PagerActivity.EXTRA_JUMP_TO_TRANSLATION, settings.wasShowingTranslation)
    startActivity(i)
  }

  private fun gotoPageDialog() {
    if (!isPaused) {
      val fm = supportFragmentManager
      val jumpDialog = JumpFragment()
      jumpDialog.show(fm, JumpFragment.TAG)
    }
  }

  fun addTag() {
    if (!isPaused) {
      val fm = supportFragmentManager
      val addTagDialog = AddTagDialog()
      addTagDialog.show(fm, AddTagDialog.TAG)
    }
  }

  fun editTag(id: Long, name: String?) {
    if (!isPaused) {
      val fm = supportFragmentManager
      val addTagDialog = newInstance(id, name!!)
      addTagDialog.show(fm, AddTagDialog.TAG)
    }
  }

  fun tagBookmarks(ids: LongArray?) {
    if (ids != null && ids.size == 1) {
      tagBookmark(ids[0])
      return
    }

    if (!isPaused) {
      val fm = supportFragmentManager
      val tagBookmarkDialog = TagBookmarkDialog.newInstance(ids)
      tagBookmarkDialog.show(fm, TagBookmarkDialog.TAG)
    }
  }

  private fun tagBookmark(id: Long) {
    if (!isPaused) {
      val fm = supportFragmentManager
      val tagBookmarkDialog = TagBookmarkDialog.newInstance(id)
      tagBookmarkDialog.show(fm, TagBookmarkDialog.TAG)
    }
  }

  override fun onAddTagSelected() {
    val fm = supportFragmentManager
    val dialog = AddTagDialog()
    dialog.show(fm, AddTagDialog.TAG)
  }

  companion object {
    const val EXTRA_SHOW_TRANSLATION_UPGRADE = "transUp"
    private const val SI_SHOWED_UPGRADE_DIALOG = "si_showed_dialog"
    private const val KEY_SELECTED_NAV_ID = "selected_nav_id"
    private const val KEY_SELECTED_NAV_TAG = "selected_nav_tag"
    private const val TAG_PENDAMPING = "tab_pendamping"
    private const val TAG_KOMUNITAS = "tab_komunitas"
    private const val TAG_EVENT = "tab_event"
    const val TAG_TILAWAH = "tab_tilawah"
    private const val TAG_PROFIL = "tab_profil"
    private var updatedTranslations = false
  }
}
