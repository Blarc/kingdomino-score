package si.blarc.ui

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import si.blarc.R
import si.blarc.env.Logger
import si.blarc.firebase.FirebaseUtils.deleteCurrentPlayerFromFirebase
import si.blarc.firebase.FirebaseUtils.removeCurrentPlayerFromGame
import si.blarc.firebase.FirebaseUtils.removeGameValueListener
import si.blarc.firebase.FirebaseUtils.setNewGameAdmin
import si.blarc.ui.fragments.LobbyFragment
import si.blarc.ui.fragments.MainFragment

/***
 * Base activity that holds all the fragments.
 * @author blarc
 */
class BaseActivity : AppCompatActivity() {
    private val logger: Logger = Logger()
    private val viewModel: BaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.container)

        // If the player is leaving the lobby
        // TODO @blarc Add "Are you sure?" message
        if (fragment is LobbyFragment) {
            removeGameValueListener(viewModel, fragment)
            leaveGame(super.onBackPressed())
        }
    }

    private fun leaveGame(doAfter: Unit) {
        // If the current player is admin
        if (viewModel.currentPlayer!!.isAdmin) {
            // Randomly pick a new admin if possible
            setNewGameAdmin(viewModel).addOnCompleteListener {
                if (it.isSuccessful) {
                    // Remove current players authentication token from firebase
                    deleteCurrentPlayerFromFirebase(viewModel).addOnCompleteListener {
                        // Reset view model's variables
                        viewModel.reset()
                        doAfter.run {  }
                    }

                } else {
                    logger.e(BaseActivity::class.java.simpleName, it.exception)
                }
            }
        } else {
            // Otherwise just remove the current user from the game
            removeCurrentPlayerFromGame(viewModel).addOnCompleteListener {
                if (it.isSuccessful) {
                    // Remove current players authentication token from firebase
                    deleteCurrentPlayerFromFirebase(viewModel).addOnCompleteListener {
                        // Reset view model's variables
                        viewModel.reset()
                        doAfter.run {  }
                    }
                } else {
                    logger.e(BaseActivity::class.java.simpleName, it.exception)
                }
            }
        }

    }
}