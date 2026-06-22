package com.quran.labs.androidquran.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.quran.labs.androidquran.QuranDataActivity
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var session: SessionManager

    data class OnboardingSlide(
        val imageRes: Int,
        val title: String,
        val description: String
    )

    private val slides = listOf(
        OnboardingSlide(
            imageRes    = R.drawable.ic_onboarding_1,
            title       = "QURAN SAKU",
            description = "Quran saku adalah super app untuk membantu kamu dalam beribadah, menghapal quran serta pengingat ibadah lainnya."
        ),
        OnboardingSlide(
            imageRes    = R.drawable.ic_onboarding_2,
            title       = "Ibadah Lebih Mudah",
            description = "Jadwal sholat, arah kiblat, dzikir harian, dan doa pilihan tersedia dalam satu aplikasi yang elegan."
        ),
        OnboardingSlide(
            imageRes    = R.drawable.ic_onboarding_3,
            title       = "Be Better",
            description = "Menyediakan semua kebutuhan ibadahmu di dalam satu aplikasi. Mulai perjalanan spiritualmu hari ini."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        setupViewPager()
        setupButtons()
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = OnboardingAdapter(this, slides)

        // Connect TabLayout dots to ViewPager2
        TabLayoutMediator(binding.tabDots, binding.viewPager) { _, _ -> }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateNavButtons(position)
            }
        })
    }

    private fun setupButtons() {
        binding.btnSkip.setOnClickListener { finishOnboarding() }

        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < slides.size - 1) {
                binding.viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }
    }

    private fun updateNavButtons(position: Int) {
        val isLast = position == slides.size - 1
        binding.btnSkip.visibility = if (isLast) View.GONE else View.VISIBLE
        binding.btnNext.text       = if (isLast) "Mulai Sekarang ✨" else "Lanjut"
    }

    private fun finishOnboarding() {
        session.setOnboardingDone()
        startActivity(Intent(this, QuranDataActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}

// ─── ViewPager2 Adapter ───────────────────────
class OnboardingAdapter(
    private val context: Context,
    private val slides: List<OnboardingActivity.OnboardingSlide>
) : RecyclerView.Adapter<OnboardingAdapter.SlideViewHolder>() {

    inner class SlideViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIllustration: ImageView = view.findViewById(R.id.ivIllustration)
        val tvTitle: TextView         = view.findViewById(R.id.tvTitle)
        val tvDescription: TextView   = view.findViewById(R.id.tvDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_onboarding_slide, parent, false)
        return SlideViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
        val slide = slides[position]
        holder.ivIllustration.setImageResource(slide.imageRes)
        holder.tvTitle.text       = slide.title
        holder.tvDescription.text = slide.description
    }

    override fun getItemCount(): Int = slides.size
}
