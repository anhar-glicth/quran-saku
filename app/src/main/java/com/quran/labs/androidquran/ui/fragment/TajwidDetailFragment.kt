package com.quran.labs.androidquran.ui.fragment

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.quran.labs.androidquran.R

class TajwidDetailFragment : Fragment() {

  private var category: String = "nun_sukun"
  private var mediaPlayer: MediaPlayer? = null

  // Data structures for holding tajwid content
  private data class TajwidRule(
      val title: String,
      val badgeText: String,
      val badgeBgColor: String,
      val badgeTextColor: String,
      val subtitle: String,
      val characters: String,
      val note: String,
      val boxHeader: String? = null
  )

  private data class TajwidExample(
      val label: String,
      val title: String,
      val arabic: String,
      val translit: String
  )

  private data class TajwidQuiz(
      val question: String,
      val options: List<String>,
      val correctIndex: Int,
      val explanation: String
  )

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    arguments?.let {
      category = it.getString(ARG_CATEGORY, "nun_sukun")
    }
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_tajwid_detail, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    view.findViewById<ImageView>(R.id.btn_back).setOnClickListener {
      parentFragmentManager.popBackStack()
    }

    setupContent(view)
  }

  private fun setupContent(view: View) {
    val txtTitle = view.findViewById<TextView>(R.id.txt_detail_title)
    val txtSubtitle = view.findViewById<TextView>(R.id.txt_detail_subtitle)
    val txtCharacters = view.findViewById<TextView>(R.id.txt_detail_characters)
    val txtCharactersLabel = view.findViewById<TextView>(R.id.txt_detail_characters_label)
    val containerRules = view.findViewById<LinearLayout>(R.id.container_rules)
    val containerExamples = view.findViewById<LinearLayout>(R.id.container_examples)
    val btnStartQuiz = view.findViewById<Button>(R.id.btn_start_quiz)

    containerRules.removeAllViews()
    containerExamples.removeAllViews()

    // 1. Get data based on selected category
    val (title, subtitle, chars, charsLabel, rules, examples, quiz) = getCategoryData()

    txtTitle.text = title
    txtSubtitle.text = subtitle
    txtCharacters.text = chars
    txtCharactersLabel.text = charsLabel

    val inflater = LayoutInflater.from(context)

    // 2. Populate rules
    for (rule in rules) {
      val ruleView = inflater.inflate(R.layout.item_tajwid_rule, containerRules, false)
      ruleView.findViewById<TextView>(R.id.txt_rule_title).text = rule.title

      val txtBadge = ruleView.findViewById<TextView>(R.id.txt_rule_badge)
      txtBadge.text = rule.badgeText
      txtBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(rule.badgeBgColor))
      txtBadge.setTextColor(Color.parseColor(rule.badgeTextColor))

      ruleView.findViewById<TextView>(R.id.txt_rule_subtitle).text = rule.subtitle
      ruleView.findViewById<TextView>(R.id.txt_rule_characters).text = rule.characters
      ruleView.findViewById<TextView>(R.id.txt_rule_note).text = rule.note

      val txtBoxHeader = ruleView.findViewById<TextView>(R.id.txt_rule_box_header)
      if (rule.boxHeader != null) {
        txtBoxHeader.visibility = View.VISIBLE
        txtBoxHeader.text = rule.boxHeader
      } else {
        txtBoxHeader.visibility = View.GONE
      }

      containerRules.addView(ruleView)
    }

    // 3. Populate examples
    for (example in examples) {
      val exampleView = inflater.inflate(R.layout.item_tajwid_example, containerExamples, false)
      exampleView.findViewById<TextView>(R.id.txt_example_label).text = example.label
      exampleView.findViewById<TextView>(R.id.txt_example_title).text = example.title
      exampleView.findViewById<TextView>(R.id.txt_example_arabic).text = example.arabic
      exampleView.findViewById<TextView>(R.id.txt_example_translit).text = example.translit

      exampleView.findViewById<Button>(R.id.btn_listen).setOnClickListener {
        playExampleAudio(example.translit)
      }

      containerExamples.addView(exampleView)
    }

    // 4. Setup Quiz
    btnStartQuiz.setOnClickListener {
      showQuizDialog(quiz)
    }
  }

  private fun playExampleAudio(title: String) {
    try {
      mediaPlayer?.release()
      mediaPlayer = MediaPlayer.create(context, R.raw.bismillah)
      mediaPlayer?.setOnCompletionListener {
        it.release()
        mediaPlayer = null
      }
      mediaPlayer?.start()
      Toast.makeText(context, "Memutar audio contoh: $title", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
      Toast.makeText(context, "Gagal memutar audio", Toast.LENGTH_SHORT).show()
    }
  }

  private fun showQuizDialog(quiz: TajwidQuiz) {
    val ctx = context ?: return
    val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_tajwid_quiz, null)

    val txtTitle = dialogView.findViewById<TextView>(R.id.quiz_title)
    val txtQuestion = dialogView.findViewById<TextView>(R.id.quiz_question)
    val optionsGroup = dialogView.findViewById<RadioGroup>(R.id.quiz_options_group)
    val feedbackCard = dialogView.findViewById<CardView>(R.id.quiz_feedback_card)
    val feedbackLayout = dialogView.findViewById<LinearLayout>(R.id.quiz_feedback_layout)
    val feedbackStatus = dialogView.findViewById<TextView>(R.id.quiz_feedback_status)
    val feedbackExplanation = dialogView.findViewById<TextView>(R.id.quiz_feedback_explanation)
    val btnCancel = dialogView.findViewById<Button>(R.id.btn_quiz_cancel)
    val btnSubmit = dialogView.findViewById<Button>(R.id.btn_quiz_submit)

    txtTitle.text = "Kuis Tajwid: $category".replace("_", " ").split(' ').joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    txtQuestion.text = quiz.question

    val optionButtons = listOf(
        dialogView.findViewById<RadioButton>(R.id.option_0),
        dialogView.findViewById<RadioButton>(R.id.option_1),
        dialogView.findViewById<RadioButton>(R.id.option_2),
        dialogView.findViewById<RadioButton>(R.id.option_3)
    )

    for (i in optionButtons.indices) {
      if (i < quiz.options.size) {
        optionButtons[i].visibility = View.VISIBLE
        optionButtons[i].text = quiz.options[i]
      } else {
        optionButtons[i].visibility = View.GONE
      }
    }

    val alertDialog = AlertDialog.Builder(ctx)
        .setView(dialogView)
        .setCancelable(false)
        .create()

    btnCancel.setOnClickListener {
      alertDialog.dismiss()
    }

    btnSubmit.setOnClickListener {
      val selectedId = optionsGroup.checkedRadioButtonId
      if (selectedId == -1) {
        Toast.makeText(ctx, "Pilih salah satu jawaban terlebih dahulu!", Toast.LENGTH_SHORT).show()
        return@setOnClickListener
      }

      // Check which option index was selected
      var selectedIndex = -1
      for (i in optionButtons.indices) {
        if (optionButtons[i].id == selectedId) {
          selectedIndex = i
          break
        }
      }

      feedbackCard.visibility = View.VISIBLE
      if (selectedIndex == quiz.correctIndex) {
        feedbackStatus.text = "Maa syaa Allah, jawaban Anda benar! 🎉"
        feedbackStatus.setTextColor(Color.parseColor("#2E7D32"))
        feedbackLayout.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9"))
      } else {
        feedbackStatus.text = "Jawaban Anda kurang tepat. 📚"
        feedbackStatus.setTextColor(Color.parseColor("#C62828"))
        feedbackLayout.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
      }

      feedbackExplanation.text = quiz.explanation
      btnSubmit.text = "Tutup"
      btnSubmit.setOnClickListener {
        alertDialog.dismiss()
      }
    }

    alertDialog.show()
  }

  private fun getCategoryData(): CategoryData {
    return when (category) {
      "nun_sukun" -> CategoryData(
          title = "Nun Sukun & Tanwin",
          subtitle = "Kuasai empat hukum utama yang mengatur pelafalan Nun mati (Sukun) atau Tanwin ketika bertemu dengan huruf-huruf Hijaiyah tertentu.",
          characters = "نْ  ً  ٍ  ٌ",
          charactersLabel = "Bentuk Nun Sukun & Tanwin",
          rules = listOf(
              TajwidRule("1. Izhar Halqi", "Jelas", "#FFEBE0", "#D86D38", "Pelafalan yang jelas dan tegas tanpa dengung.", "ء ه ع ح غ kh", "*Bacalah bunyi Nun/Tanwin secara jelas tanpa dengungan (ghunnah).*"),
              TajwidRule("2. Idgham", "Melebur", "#E3F2FD", "#1E88E5", "Meleburkan bunyi ke huruf berikutnya.", "ي ن م و ل ر", "*Bunyi Nun/Tanwin dimasukkan ke huruf berikutnya. Terbagi menjadi Bighunnah (dengan dengung) dan Bilaghunnah (tanpa dengung).*"),
              TajwidRule("3. Iqlab", "Mengubah", "#F3E5F5", "#8E24AA", "Mengubah bunyi menjadi Mim.", "ب", "*Bunyi Nun/Tanwin diubah menjadi bunyi Mim (م) disertai dengan dengungan samar.*"),
              TajwidRule("4. Ikhfa Haqiqi", "Samar", "#E8F5E9", "#2E7D32", "Menyamarkan bunyi bacaan.", "ت ث ج د ذ ز س ش ص ض ط ظ ف ق ك", "*Samarkan bunyi Nun dan tahan dengungan selama 2 harakat.*", "15 Huruf Lainnya")
          ),
          examples = listOf(
              TajwidExample("Contoh: Izhar", "Pelafalan Jelas", "مَنْ آمَنَ", "Man Aamana"),
              TajwidExample("Contoh: Idgham Bighunnah", "Melebur dengan Dengung", "مَنْ يَقُولُ", "May Yaqulu"),
              TajwidExample("Contoh: Iqlab", "Mengubah ke 'Mim'", "مِنْ بَعْدِ", "Mim Ba'di")
          ),
          quiz = TajwidQuiz(
              question = "Hukum tajwid apakah jika Nun Sukun atau Tanwin bertemu dengan huruf Ba (ب)?",
              options = listOf("Izhar Halqi", "Idgham Bighunnah", "Iqlab", "Ikhfa Haqiqi"),
              correctIndex = 2,
              explanation = "Ketika Nun Sukun atau Tanwin bertemu dengan huruf Ba (ب), suaranya diubah menjadi bunyi Mim disertai dengungan yang rapat (Iqlab)."
          )
      )
      "mim_sukun" -> CategoryData(
          title = "Mim Sukun",
          subtitle = "Pelajari tiga hukum utama yang mengatur pelafalan huruf Mim mati (Sukun) saat bertemu dengan huruf Hijaiyah.",
          characters = "مْ",
          charactersLabel = "Bentuk Mim Sukun",
          rules = listOf(
              TajwidRule("1. Ikhfa Syafawi", "Samar Syafawi", "#FFEBE0", "#D86D38", "Menyamarkan pelafalan Mim mati di bibir.", "ب", "*Membaca Mim mati dengan samar di bibir disertai dengungan (ghunnah) saat bertemu Ba.*"),
              TajwidRule("2. Idgham Mimi / Mitslain", "Melebur Mimi", "#E3F2FD", "#1E88E5", "Meleburkan dua huruf Mim.", "م", "*Memasukkan Mim mati ke huruf Mim berikutnya disertai dengungan yang rapat.*"),
              TajwidRule("3. Izhar Syafawi", "Jelas Syafawi", "#F3E5F5", "#8E24AA", "Pelafalan jelas di bibir.", "Semua huruf selain ب dan م", "*Membaca Mim mati secara jelas tanpa dengung saat bertemu huruf selain Ba dan Mim.*")
          ),
          examples = listOf(
              TajwidExample("Contoh: Ikhfa Syafawi", "Samar Syafawi", "تَرْمِيهِمْ بِحِجَارَةٍ", "Tarmihim bihijaratin"),
              TajwidExample("Contoh: Idgham Mimi", "Melebur Mimi", "لَهُمْ مَا يَشَاؤُونَ", "Lahum ma yasha'un"),
              TajwidExample("Contoh: Izhar Syafawi", "Jelas Syafawi", "لَمْ يَلِدْ", "Lam yalid")
          ),
          quiz = TajwidQuiz(
              question = "Hukum apakah jika Mim Sukun bertemu dengan huruf Ba (ب)?",
              options = listOf("Ikhfa Syafawi", "Izhar Syafawi", "Idgham Mimi"),
              correctIndex = 0,
              explanation = "Hukum Mim Sukun bertemu dengan huruf Ba (ب) disebut Ikhfa Syafawi, yang dibaca samar-samar di bibir dengan dengungan."
          )
      )
      "alif_lam" -> CategoryData(
          title = "Alif Lam",
          subtitle = "Memahami perbedaan cara membaca Alif Lam (ال) ketika bertemu huruf Qamariyah (jelas) dan Syamsiyah (melebur).",
          characters = "الـ",
          charactersLabel = "Bentuk Alif Lam Tarikh",
          rules = listOf(
              TajwidRule("1. Alif Lam Qamariyah", "Jelas / Sukun", "#FFEBE0", "#D86D38", "Alif Lam dibaca jelas.", "ء ب ج ح خ ع غ ف ق ك م w h y", "*Alif Lam dibaca dengan jelas (sukunnya terdengar nyata) sebelum huruf Qamariyah.*"),
              TajwidRule("2. Alif Lam Syamsiyah", "Melebur / Tasydid", "#E3F2FD", "#1E88E5", "Alif Lam tidak dibaca melainkan melebur.", "ت ث د ذ ر ز س ش ص ض ط ظ ل ن", "*Suara Alif Lam dileburkan langsung masuk ke huruf berikutnya yang bertasydid.*")
          ),
          examples = listOf(
              TajwidExample("Contoh: Al-Qamariyah", "Dibaca Jelas", "الْحَمْدُ", "Al-Hamdu"),
              TajwidExample("Contoh: Al-Syamsiyah", "Dibaca Melebur", "الرَّحْمَنِ", "Ar-Rahman")
          ),
          quiz = TajwidQuiz(
              question = "Bagaimanakah cara membaca Alif Lam Qamariyah?",
              options = listOf("Jelas dan Nyata", "Samar-samar", "Melebur ke huruf berikutnya"),
              correctIndex = 0,
              explanation = "Alif Lam Qamariyah dibaca secara jelas dan nyata karena huruf L-nya disukunkan (terdengar jelas)."
          )
      )
      "hukum_mad" -> CategoryData(
          title = "Hukum Mad",
          subtitle = "Aturan memanjangkan suara ketukan atau harakat saat membaca Al-Qur'an agar sesuai dengan tajwid asli.",
          characters = "ا  و  ي",
          charactersLabel = "Huruf Mad (Alif, Wawu, Ya)",
          rules = listOf(
              TajwidRule("1. Mad Asli / Thabi'i", "Pokok", "#FFEBE0", "#D86D38", "Mad dasar 2 harakat.", "Alif, Wawu, Ya (dengan harakat sesuai)", "*Memanjangkan bacaan sebanyak 2 harakat secara alami tanpa adanya hamzah atau sukun.*"),
              TajwidRule("2. Mad Wajib Muttasil", "Wajib", "#E3F2FD", "#1E88E5", "Bertemu Hamzah dalam satu kata.", "Mad Asli + Hamzah (1 kata)", "*Wajib dibaca panjang 4 sampai 5 harakat.*"),
              TajwidRule("3. Mad Jaiz Munfasil", "Jaiz", "#F3E5F5", "#8E24AA", "Bertemu Hamzah di lain kata.", "Mad Asli + Hamzah (beda kata)", "*Boleh dibaca panjang 2, 4, atau 5 harakat.*"),
              TajwidRule("4. Mad Arid Lissukun", "Waqaf", "#E8F5E9", "#2E7D32", "Mad di akhir ayat sebelum waqaf.", "Mad Asli + Huruf mati karena Waqaf", "*Dibaca panjang 2, 4, atau 6 harakat di akhir kata yang dihentikan.*")
          ),
          examples = listOf(
              TajwidExample("Contoh: Mad Thabi'i", "Mad Dasar", "قَالَ", "Qaala"),
              TajwidExample("Contoh: Mad Wajib", "Mad Wajib Muttasil", "جَاءَ", "Jaa'a"),
              TajwidExample("Contoh: Mad Jaiz", "Mad Jaiz Munfasil", "بِمَا أُنْزِلَ", "Bimaa unzila")
          ),
          quiz = TajwidQuiz(
              question = "Berapa harakatkah panjang bacaan Mad Wajib Muttasil?",
              options = listOf("2 harakat", "4 sampai 5 harakat", "6 harakat"),
              correctIndex = 1,
              explanation = "Mad Wajib Muttasil wajib dibaca panjang sepanjang 4 sampai 5 harakat ketika bersambung dengan hamzah dalam satu kata."
          )
      )
      else -> CategoryData(
          title = "Idgham & Qalqalah",
          subtitle = "Teknik meleburkan suara ke huruf berikutnya serta memantulkan bunyi huruf-huruf tertentu saat sukun.",
          characters = "ق ط ب ج د",
          charactersLabel = "Huruf Qalqalah (Baju Di Toko)",
          rules = listOf(
              TajwidRule("1. Qalqalah Sugra", "Pantulan Kecil", "#FFEBE0", "#D86D38", "Pantulan di tengah kata.", "ق ط ب ج د (sukun asli)", "*Pantulan suara yang tipis/ringan karena huruf berada di tengah-tengah kata.*"),
              TajwidRule("2. Qalqalah Kubra", "Pantulan Besar", "#E3F2FD", "#1E88E5", "Pantulan di akhir kata (waqaf).", "ق ط ب ج د (dihentikan)", "*Pantulan suara yang kuat dan tebal karena huruf berada di akhir kata yang dibaca sukun.*"),
              TajwidRule("3. Idgham Mutamatsilain", "Melebur Sama", "#F3E5F5", "#8E24AA", "Pertemuan dua huruf sejenis.", "Pertemuan dua huruf sama (mati & hidup)", "*Memasukkan huruf pertama yang mati ke huruf kedua yang sejenis.*")
          ),
          examples = listOf(
              TajwidExample("Contoh: Sugra", "Pantulan Kecil (Sugra)", "يَقْطَعُونَ", "Yaqtha'uuna"),
              TajwidExample("Contoh: Kubra", "Pantulan Besar (Kubra)", "عَذَابٌ شَدِيدٌ", "Syadiid (diwaqafkan)"),
              TajwidExample("Contoh: Mutamatsilain", "Melebur Sama", "اذْهَبْ بِكِتَابِي", "Idzhab bikitaabi")
          ),
          quiz = TajwidQuiz(
              question = "Hukum apakah yang memantulkan bunyi huruf tajwid di tengah-tengah kata?",
              options = listOf("Qalqalah Sugra", "Qalqalah Kubra", "Idgham Mutamatsilain"),
              correctIndex = 0,
              explanation = "Qalqalah Sugra terjadi apabila huruf qalqalah mati asli berada di tengah kata dan dipantulkan secara ringan."
          )
      )
    }
  }

  override fun onResume() {
    super.onResume()
    // Hide parent activity toolbar area to ensure no double toolbars are visible
    (activity as? AppCompatActivity)?.supportActionBar?.hide()
    activity?.findViewById<View>(R.id.toolbar_area)?.visibility = View.GONE
  }

  override fun onPause() {
    super.onPause()
    // Restore parent activity toolbar area when leaving
    (activity as? AppCompatActivity)?.supportActionBar?.show()
    activity?.findViewById<View>(R.id.toolbar_area)?.visibility = View.VISIBLE
  }

  override fun onDestroy() {
    mediaPlayer?.release()
    mediaPlayer = null
    super.onDestroy()
  }

  private data class CategoryData(
      val title: String,
      val subtitle: String,
      val characters: String,
      val charactersLabel: String,
      val rules: List<TajwidRule>,
      val examples: List<TajwidExample>,
      val quiz: TajwidQuiz
  )

  companion object {
    private const val ARG_CATEGORY = "category"

    fun newInstance(category: String): TajwidDetailFragment {
      val fragment = TajwidDetailFragment()
      val args = Bundle()
      args.putString(ARG_CATEGORY, category)
      fragment.arguments = args
      return fragment
    }
  }
}
