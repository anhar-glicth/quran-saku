package com.quran.labs.androidquran.ui.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.quran.data.core.QuranInfo
import com.quran.data.model.SuraAyah
import com.quran.labs.androidquran.QuranApplication
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.data.QuranDisplayData
import com.quran.labs.androidquran.model.translation.ArabicDatabaseUtils
import com.quran.labs.androidquran.ui.helpers.JumpDestination
import dev.zacsweers.metro.Inject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

class TahfidzRecitationFragment : Fragment() {

  @Inject
  lateinit var arabicDatabaseUtils: ArabicDatabaseUtils

  @Inject
  lateinit var quranInfo: QuranInfo

  @Inject
  lateinit var quranDisplayData: QuranDisplayData

  private var speechRecognizer: SpeechRecognizer? = null
  private var isListening = false
  private var targetVersesText = ""
  private var selectedSura = 1
  private var selectedStartAyat = 1
  private var selectedEndAyat = 5

  // Views
  private lateinit var layoutSetup: View
  private lateinit var layoutRecitationWorkspace: View
  private lateinit var spinSurah: Spinner
  private lateinit var editAyatStart: EditText
  private lateinit var editAyatEnd: EditText
  private lateinit var txtRecitationHeader: TextView
  private lateinit var txtQuranVersesBoard: TextView
  private lateinit var txtVoicePreview: TextView
  private lateinit var cardResultAccuracy: CardView
  private lateinit var txtAccuracyBadge: TextView
  private lateinit var txtAccuracyScore: TextView
  private lateinit var txtRecitationStatus: TextView
  private lateinit var btnMic: FloatingActionButton

  private val requestPermissionLauncher = registerForActivityResult(
      ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      startListening()
    } else {
      Toast.makeText(context, "Izin mikrofon diperlukan untuk setor hafalan.", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    (context.applicationContext as QuranApplication).applicationComponent.inject(this)
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_tahfidz_recitation, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // Bind Views
    layoutSetup = view.findViewById(R.id.layout_setup)
    layoutRecitationWorkspace = view.findViewById(R.id.layout_recitation_workspace)
    spinSurah = view.findViewById(R.id.spin_surah)
    editAyatStart = view.findViewById(R.id.edit_ayat_start)
    editAyatEnd = view.findViewById(R.id.edit_ayat_end)
    txtRecitationHeader = view.findViewById(R.id.txt_recitation_header)
    txtQuranVersesBoard = view.findViewById(R.id.txt_quran_verses_board)
    txtVoicePreview = view.findViewById(R.id.txt_voice_preview)
    cardResultAccuracy = view.findViewById(R.id.card_result_accuracy)
    txtAccuracyBadge = view.findViewById(R.id.txt_accuracy_badge)
    txtAccuracyScore = view.findViewById(R.id.txt_accuracy_score)
    txtRecitationStatus = view.findViewById(R.id.txt_recitation_status)
    btnMic = view.findViewById(R.id.btn_mic)

    // Back Button
    view.findViewById<ImageView>(R.id.btn_back).setOnClickListener {
      if (layoutRecitationWorkspace.visibility == View.VISIBLE) {
        layoutRecitationWorkspace.visibility = View.GONE
        layoutSetup.visibility = View.VISIBLE
        resetWorkspace()
      } else {
        parentFragmentManager.popBackStack()
      }
    }

    // Populate Surah Spinner
    val suraList = (1..114).map { suraNum ->
      val name = quranDisplayData.getSuraName(requireContext(), suraNum, false, true)
      "$suraNum. $name"
    }
    val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, suraList)
    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    spinSurah.adapter = spinnerAdapter

    spinSurah.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        selectedSura = position + 1
        val maxAyah = quranInfo.getNumberOfAyahs(selectedSura)
        editAyatStart.setText("1")
        editAyatEnd.setText(maxAyah.toString())
      }
      override fun onNothingSelected(parent: AdapterView<*>?) {}
    }

    // Confirm Start Recitation
    view.findViewById<Button>(R.id.btn_start_recitation_confirm).setOnClickListener {
      val startVal = editAyatStart.text.toString().toIntOrNull() ?: 1
      val endVal = editAyatEnd.text.toString().toIntOrNull() ?: 1
      val maxAyah = quranInfo.getNumberOfAyahs(selectedSura)

      if (startVal < 1 || startVal > maxAyah || endVal < 1 || endVal > maxAyah || startVal > endVal) {
        Toast.makeText(context, "Rentang ayat tidak valid (Sura ini memiliki $maxAyah ayat).", Toast.LENGTH_SHORT).show()
        return@setOnClickListener
      }

      selectedStartAyat = startVal
      selectedEndAyat = endVal
      startRecitationSession()
    }

    // Setup Speech Recognizer
    if (SpeechRecognizer.isRecognitionAvailable(requireContext())) {
      speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
      speechRecognizer?.setRecognitionListener(createSpeechListener())
    } else {
      Toast.makeText(context, "Speech Recognition tidak tersedia di perangkat ini.", Toast.LENGTH_LONG).show()
    }

    // Mic Button Click
    btnMic.setOnClickListener {
      val permissionCheck = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
      if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
        if (isListening) {
          stopListening()
        } else {
          startListening()
        }
      } else {
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
      }
    }
  }

  private fun startRecitationSession() {
    layoutSetup.visibility = View.GONE
    layoutRecitationWorkspace.visibility = View.VISIBLE
    resetWorkspace()

    val suraName = quranDisplayData.getSuraName(requireContext(), selectedSura, false, true)
    txtRecitationHeader.text = "$suraName (Ayat $selectedStartAyat - $selectedEndAyat)"
    txtQuranVersesBoard.text = "Memuat ayat..."

    val startAyah = SuraAyah(selectedSura, selectedStartAyat)
    val endAyah = SuraAyah(selectedSura, selectedEndAyat)

    arabicDatabaseUtils.getVerses(startAyah, endAyah)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ verses ->
          val combinedText = verses.joinToString(" ") { it.text }
          targetVersesText = combinedText
          txtQuranVersesBoard.text = combinedText
          txtQuranVersesBoard.setTextColor(Color.parseColor("#888888"))
        }, { error ->
          txtQuranVersesBoard.text = "Gagal memuat ayat Al-Qur'an."
        })
  }

  private fun startListening() {
    if (speechRecognizer == null) {
      Toast.makeText(context, "Pengenal suara tidak siap.", Toast.LENGTH_SHORT).show()
      return
    }

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
      putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar")
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar")
      putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ar")
      putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }

    txtVoicePreview.text = "Mulai bersuara..."
    txtRecitationStatus.text = "Mendengarkan bacaan Anda..."
    btnMic.setImageResource(android.R.drawable.ic_media_pause)
    isListening = true
    cardResultAccuracy.visibility = View.GONE

    speechRecognizer?.startListening(intent)
  }

  private fun stopListening() {
    speechRecognizer?.stopListening()
    btnMic.setImageResource(android.R.drawable.ic_btn_speak_now)
    isListening = false
    txtRecitationStatus.text = "Menganalisis bacaan..."
  }

  private fun resetWorkspace() {
    isListening = false
    targetVersesText = ""
    txtVoicePreview.text = "..."
    cardResultAccuracy.visibility = View.GONE
    txtRecitationStatus.text = "Ketuk mikrofon untuk mulai membaca"
    btnMic.setImageResource(android.R.drawable.ic_btn_speak_now)
  }

  private fun stripHarakat(text: String): String {
    val diacriticsRegex = ("[\u064B-\u065F\u0670\u06D6-\u06ED]").toRegex()
    var clean = text.replace(diacriticsRegex, "")
    clean = clean.replace("[إأآٱ]".toRegex(), "ا")
    clean = clean.replace("ى".toRegex(), "ي")
    clean = clean.replace("ة".toRegex(), "ه")
    return clean
  }

  private fun processRecitationCheck(spokenText: String) {
    if (targetVersesText.isEmpty()) return

    val cleanTargetText = stripHarakat(targetVersesText)
    val cleanSpokenText = stripHarakat(spokenText)

    val targetWords = targetVersesText.split("\\s+".toRegex()).filter { it.isNotEmpty() }
    val cleanTargetWords = cleanTargetText.split("\\s+".toRegex()).filter { it.isNotEmpty() }
    val spokenWords = cleanSpokenText.split("\\s+".toRegex()).filter { it.isNotEmpty() }

    val n = cleanTargetWords.size
    val m = spokenWords.size

    val dp = Array(n + 1) { IntArray(m + 1) }
    for (i in 1..n) {
      for (j in 1..m) {
        if (cleanTargetWords[i - 1] == spokenWords[j - 1]) {
          dp[i][j] = dp[i - 1][j - 1] + 1
        } else {
          dp[i][j] = maxOf(dp[i - 1][j], dp[i][j - 1])
        }
      }
    }

    val matchedTargetIndices = HashSet<Int>()
    var i = n
    var j = m
    while (i > 0 && j > 0) {
      if (cleanTargetWords[i - 1] == spokenWords[j - 1]) {
        matchedTargetIndices.add(i - 1)
        i--
        j--
      } else if (dp[i - 1][j] >= dp[i][j - 1]) {
        i--
      } else {
        j--
      }
    }

    val builder = SpannableStringBuilder()
    for (idx in targetWords.indices) {
      val word = targetWords[idx]
      val start = builder.length
      builder.append(word)
      val end = builder.length

      val color = if (matchedTargetIndices.contains(idx)) {
        Color.parseColor("#4CAF50") // Green
      } else {
        Color.parseColor("#F44336") // Red
      }
      builder.setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

      if (idx < targetWords.size - 1) {
        builder.append(" ")
      }
    }

    txtQuranVersesBoard.text = builder

    val accuracy = if (n > 0) (matchedTargetIndices.size.toFloat() / n.toFloat() * 100).toInt() else 0
    cardResultAccuracy.visibility = View.VISIBLE
    if (accuracy >= 80) {
      cardResultAccuracy.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
      txtAccuracyBadge.text = "✅ Lulus"
      txtAccuracyBadge.setTextColor(Color.parseColor("#2E7D32"))
      txtAccuracyScore.text = "Akurasi: $accuracy% - Setoran diterima!"
      txtAccuracyScore.setTextColor(Color.parseColor("#1B5E20"))
      txtRecitationStatus.text = "Alhamdulillah! Setoran hafalan selesai."
    } else {
      cardResultAccuracy.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
      txtAccuracyBadge.text = "❌ Belum Lulus"
      txtAccuracyBadge.setTextColor(Color.parseColor("#C62828"))
      txtAccuracyScore.text = "Akurasi: $accuracy% - Coba ulangi hafalan Anda."
      txtAccuracyScore.setTextColor(Color.parseColor("#B71C1C"))
      txtRecitationStatus.text = "Ketuk mikrofon untuk mencoba kembali."
    }
  }

  private fun createSpeechListener(): RecognitionListener {
    return object : RecognitionListener {
      override fun onReadyForSpeech(params: Bundle?) {}
      override fun onBeginningOfSpeech() {}
      override fun onRmsChanged(rmsdB: Float) {}
      override fun onBufferReceived(buffer: ByteArray?) {}
      override fun onEndOfSpeech() {
        btnMic.setImageResource(android.R.drawable.ic_btn_speak_now)
        isListening = false
      }

      override fun onError(error: Int) {
        isListening = false
        btnMic.setImageResource(android.R.drawable.ic_btn_speak_now)
        val msg = when (error) {
          SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
          SpeechRecognizer.ERROR_CLIENT -> "Client error"
          SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions missing"
          SpeechRecognizer.ERROR_NETWORK -> "Network error"
          SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
          SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
          SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
          SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
          else -> "Recognizer error"
        }
        txtRecitationStatus.text = "Gagal terdeteksi: $msg"
      }

      override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
          val text = matches[0]
          txtVoicePreview.text = text
          processRecitationCheck(text)
        } else {
          txtVoicePreview.text = "(Tidak terdeteksi)"
          txtRecitationStatus.text = "Gagal mendeteksi ucapan."
        }
      }

      override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
          txtVoicePreview.text = matches[0]
        }
      }

      override fun onEvent(eventType: Int, params: Bundle?) {}
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    speechRecognizer?.destroy()
  }

  companion object {
    fun newInstance(): TahfidzRecitationFragment = TahfidzRecitationFragment()
  }
}
