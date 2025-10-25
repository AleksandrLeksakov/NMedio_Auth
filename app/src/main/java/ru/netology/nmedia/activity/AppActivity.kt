package ru.netology.nmedia.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.navigation.findNavController
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.NewPostFragment.Companion.textArg
import ru.netology.nmedia.service.PushService
import ru.netology.nmedia.viewmodel.AuthViewModel


//@AndroidEntryPoint

@AndroidEntryPoint

class AppActivity : AppCompatActivity(R.layout.activity_app) {

    private val authViewModel by viewModels<AuthViewModel>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            println("✅ Notification permission granted")
        } else {
            println("❌ Notification permission denied")
            Snackbar.make(
                findViewById(android.R.id.content),
                "Уведомления отключены",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Создаем канал уведомлений
        PushService.createNotificationChannel(this)

        // 2. Запрашиваем разрешение на уведомления
        requestNotificationsPermission()

        addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(
                    menu: Menu,
                    menuInflater: MenuInflater
                ) {
                    menuInflater.inflate(R.menu.auth_menu, menu)

                    authViewModel.isAuthorized.observe(this@AppActivity) { authorized ->
                        menu.setGroupVisible(R.id.unauthenticated, !authorized)
                        menu.setGroupVisible(R.id.authenticated, authorized)
                    }
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                    when (menuItem.itemId) {
                        R.id.signin -> {
                            findNavController(R.id.nav_host_fragment)
                                .navigate(R.id.action_feedFragment_to_signInFragment)
                            true
                        }
                        R.id.signup -> {
                            Snackbar.make(
                                findViewById(android.R.id.content),
                                "Registration will be implemented later",
                                Snackbar.LENGTH_SHORT
                            ).show()
                            true
                        }
                        R.id.logout -> {
                            authViewModel.logout()
                            Snackbar.make(
                                findViewById(android.R.id.content),
                                getString(R.string.signed_out),
                                Snackbar.LENGTH_SHORT
                            ).show()
                            true
                        }
                        else -> false
                    }
            }
        )

        intent?.let {
            if (it.action != Intent.ACTION_SEND) {
                return@let
            }

            val text = it.getStringExtra(Intent.EXTRA_TEXT)
            if (text?.isNotBlank() != true) {
                return@let
            }

            intent.removeExtra(Intent.EXTRA_TEXT)
            findNavController(R.id.nav_host_fragment)
                .navigate(
                    R.id.action_feedFragment_to_newPostFragment,
                    Bundle().apply {
                        textArg = text
                    }
                )
        }

        // 3. Проверяем Google Play Services и получаем FCM токен
        checkGoogleApiAvailability()
    }

    private fun requestNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    println("✅ Notification permission already granted")
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun checkGoogleApiAvailability() {
        with(GoogleApiAvailability.getInstance()) {
            val code = isGooglePlayServicesAvailable(this@AppActivity)
            if (code == ConnectionResult.SUCCESS) {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    println("🔥 FCM Token: $token")
                }
                return@with
            }
            if (isUserResolvableError(code)) {
                getErrorDialog(this@AppActivity, code, 9000)?.show()
                return
            }
            Toast.makeText(this@AppActivity, R.string.google_play_unavailable, Toast.LENGTH_LONG)
                .show()
        }
    }
}