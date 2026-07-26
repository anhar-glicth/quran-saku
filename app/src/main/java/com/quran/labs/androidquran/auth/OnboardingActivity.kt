package com.quran.labs.androidquran.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
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
            title       = "STRAVA QURAN",
            description = "Strava Quran adalah super app untuk membantu kamu dalam beribadah, menghafal quran serta pengingat ibadah lainnya."
        ),
        OnboardingSlide(
            imageRes    = R.drawable.ic_onboarding_2,
            title       = "Track Tilawah & Target",
            description = "Catat dan lacak progres tilawah harianmu seperti Strava, raih konsistensi membaca Al-Qur'an setiap hari."
        ),
        OnboardingSlide(
            imageRes    = R.drawable.ic_onboarding_3,
            title       = "Komunitas Pejuang Quran",
            description = "Bergabung dalam grup ngaji, ikuti leaderboard pejuang tilawah, dan saling menyemangati dalam kebaikan."
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

        setupDots(slides.size)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateNavButtons(position)
                updateDots(position)
            }
        })
    }

    private fun setupDots(count: Int) {
        binding.layoutDots.removeAllViews()
        val density = resources.displayMetrics.density
        val sizePx = (10 * density).toInt()
        val marginPx = (4 * density).toInt()

        for (i in 0 until count) {
            val dot = View(this).apply {
                val params = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    setMargins(marginPx, 0, marginPx, 0)
                }
                layoutParams = params
                background = ContextCompat.getDrawable(
                    this@OnboardingActivity,
                    if (i == 0) R.drawable.dot_active else R.drawable.dot_inactive
                )
            }
            binding.layoutDots.addView(dot)
        }
    }

    private fun updateDots(position: Int) {
        for (i in 0 until binding.layoutDots.childCount) {
            val dot = binding.layoutDots.getChildAt(i)
            dot.background = ContextCompat.getDrawable(
                this,
                if (i == position) R.drawable.dot_active else R.drawable.dot_inactive
            )
        }
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
