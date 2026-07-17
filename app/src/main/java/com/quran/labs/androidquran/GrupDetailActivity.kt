package com.quran.labs.androidquran

import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.tabs.TabLayout
import com.quran.labs.androidquran.auth.AuthClient
import com.quran.labs.androidquran.auth.SessionManager
import com.quran.labs.androidquran.model.GrupMember
import com.quran.labs.androidquran.model.GrupNgaji
import com.quran.labs.androidquran.model.ReadingRelay
import com.quran.labs.androidquran.ui.PagerActivity
import com.quran.labs.androidquran.ui.adapter.GrupDetailAdapter
import kotlinx.coroutines.launch

class GrupDetailActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: GrupDetailAdapter

    private var groupId: Int = 0
    private var isGroupAdmin: Boolean = false
    private var groupInfo: GrupNgaji? = null
    private var lastRelay: ReadingRelay? = null

    private lateinit var tvName: TextView
    private lateinit var tvCode: TextView
    private lateinit var tvInitials: TextView
    private lateinit var tvTargetDesc: TextView
    private lateinit var tvProgressPct: TextView
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var progressDetail: ProgressBar
    private lateinit var rvContent: RecyclerView
    private lateinit var tabLayout: TabLayout
    private lateinit var btnEditGroup: ImageButton
    private lateinit var btnPendingRequests: Button
    private lateinit var btnLanjutkan: Button

    private var currentMembersList: List<GrupMember> = emptyList()
    private var currentRelayList: List<ReadingRelay> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_grup_detail)

        sessionManager = SessionManager(this)
        groupId = intent.getIntExtra("group_id", 0)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = "Detail Grup"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Bind Views
        tvName = findViewById(R.id.txt_detail_name)
        tvCode = findViewById(R.id.txt_detail_code)
        tvInitials = findViewById(R.id.txt_detail_initials)
        tvTargetDesc = findViewById(R.id.txt_target_desc)
        tvProgressPct = findViewById(R.id.txt_progress_pct)
        progressIndicator = findViewById(R.id.detail_progress_indicator)
        progressDetail = findViewById(R.id.progress_detail)
        rvContent = findViewById(R.id.rv_detail_content)
        tabLayout = findViewById(R.id.tab_layout)
        btnEditGroup = findViewById(R.id.btn_edit_group)
        btnPendingRequests = findViewById(R.id.btn_pending_requests)
        btnLanjutkan = findViewById(R.id.btn_lanjutkan_tilawah)

        // Setup Adapter
        adapter = GrupDetailAdapter()
        rvContent.layoutManager = LinearLayoutManager(this)
        rvContent.adapter = adapter

        // TabLayout Listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> adapter.setRelayData(currentRelayList)
                    1 -> adapter.setMemberData(currentMembersList)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        btnLanjutkan.setOnClickListener {
            lanjutkanAjianTerakhir()
        }

        btnEditGroup.setOnClickListener {
            showEditGroupNameDialog()
        }

        btnPendingRequests.setOnClickListener {
            showPendingRequestsDialog()
        }

        // Fetch detail
        fetchGroupDetail()
    }

    private fun fetchGroupDetail() {
        progressDetail.visibility = View.VISIBLE
        val userId = sessionManager.getUserId()
        
        lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.getGroupDetail(userId = userId, groupId = groupId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()!!
                    groupInfo = body.group
                    val progressPct = body.progressPercent ?: 0
                    currentMembersList = body.members ?: emptyList()
                    currentRelayList = body.relay ?: emptyList()

                    groupInfo?.let { group ->
                        tvName.text = group.name
                        tvCode.text = "ID: ${group.groupCode}"
                        tvInitials.text = if (group.name.isNotEmpty()) group.name.take(2).uppercase() else "G"
                        tvTargetDesc.text = "Target: ${group.targetKhatam}x Khatam (${group.durationDays} Hari)"
                        tvProgressPct.text = "$progressPct% Selesai"
                        progressIndicator.progress = progressPct

                        // Store group status to SharedPreferences for automatic progress updates
                        val prefs = PreferenceManager.getDefaultSharedPreferences(this@GrupDetailActivity)
                        prefs.edit().putString("group_member_status", "active").apply()

                        // Check if user is creator/admin
                        isGroupAdmin = (group.creatorId == sessionManager.getUserId())
                        if (isGroupAdmin) {
                            btnEditGroup.visibility = View.VISIBLE
                            btnPendingRequests.visibility = View.VISIBLE
                        } else {
                            btnEditGroup.visibility = View.GONE
                            btnPendingRequests.visibility = View.GONE
                        }
                    }

                    // Ambil bacaan relay teratas/terakhir
                    lastRelay = currentRelayList.firstOrNull()

                    // Update UI list based on current active tab
                    if (tabLayout.selectedTabPosition == 0) {
                        adapter.setRelayData(currentRelayList)
                    } else {
                        adapter.setMemberData(currentMembersList)
                    }
                } else {
                    Toast.makeText(this@GrupDetailActivity, "Gagal memuat detail grup", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@GrupDetailActivity, "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
            } finally {
                progressDetail.visibility = View.GONE
            }
        }
    }

    private fun lanjutkanAjianTerakhir() {
        // Jika ada relay terdaftar, lompat ke page tersebut, jika belum ada, default ke Al-Fatihah/halaman 1
        val pageNum = groupInfo?.currentPage ?: 1
        val intent = Intent(this, PagerActivity::class.java).apply {
            putExtra("page", pageNum)
            putExtra("grup_ngaji_id", groupId)
        }
        startActivity(intent)
    }

    private fun showEditGroupNameDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_group, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_edit_group_name)
        val etDesc = dialogView.findViewById<EditText>(R.id.et_edit_group_desc)

        etName.setText(groupInfo?.name ?: "")
        etDesc.setText(groupInfo?.description ?: "")

        AlertDialog.Builder(this)
            .setTitle("✏️ Edit Detail Grup")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val newName = etName.text.toString().trim()
                val newDesc = etDesc.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateGroupName(newName, newDesc)
                } else {
                    Toast.makeText(this, "Nama grup tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateGroupName(newName: String, newDesc: String) {
        val userId = sessionManager.getUserId()
        lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.updateGroup(
                    adminId = userId,
                    groupId = groupId,
                    name = newName,
                    description = newDesc,
                    photoBase64 = ""
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@GrupDetailActivity, "Grup berhasil diupdate!", Toast.LENGTH_SHORT).show()
                    fetchGroupDetail()
                } else {
                    Toast.makeText(this@GrupDetailActivity, response.body()?.message ?: "Gagal update grup", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showPendingRequestsDialog() {
        val userId = sessionManager.getUserId()
        lifecycleScope.launch {
            try {
                val response = AuthClient.apiService.getPendingMembers(userId = userId, groupId = groupId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val pendingList = response.body()!!.data ?: emptyList()
                    if (pendingList.isEmpty()) {
                        Toast.makeText(this@GrupDetailActivity, "Tidak ada permintaan gabung pending.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val names = pendingList.map { it.name }.toTypedArray()
                    AlertDialog.Builder(this@GrupDetailActivity)
                        .setTitle("Permintaan Join Pending")
                        .setItems(names) { _, index ->
                            val selectedUser = pendingList[index]
                            showApproveRejectDialog(selectedUser)
                        }
                        .setNegativeButton("Tutup", null)
                        .show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showApproveRejectDialog(user: GrupMember) {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Permintaan")
            .setMessage("Setujui ${user.name} untuk bergabung ke grup?")
            .setPositiveButton("Terima (Acc)") { _, _ ->
                respondJoinRequest(user.id, "approve")
            }
            .setNegativeButton("Tolak") { _, _ ->
                respondJoinRequest(user.id, "reject")
            }
            .setNeutralButton("Batal", null)
            .show()
    }

    private fun respondJoinRequest(targetUserId: Int, action: String) {
        val userId = sessionManager.getUserId()
        lifecycleScope.launch {
            try {
                val actionParam = if (action == "approve") "approve_member" else "reject_member"
                val response = AuthClient.apiService.respondJoinRequest(
                    action = actionParam,
                    adminId = userId,
                    targetUserId = targetUserId,
                    groupId = groupId
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@GrupDetailActivity, "Berhasil memproses permintaan!", Toast.LENGTH_SHORT).show()
                    fetchGroupDetail()
                } else {
                    Toast.makeText(this@GrupDetailActivity, response.body()?.message ?: "Gagal memproses", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
