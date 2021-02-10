package si.blarc.env

import android.app.AlertDialog
import android.content.pm.PackageManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import si.blarc.R

object UIUtils {

    /**
     * Replaces current fragment with [fragmentClass].
     * @param [fragmentActivity] current fragment's activity
     * @param [fragmentClass]    target fragment
     * @author blarc
     */
    fun replaceFragment(fragmentActivity: FragmentActivity, fragmentClass: Class<*>) {
        var fragment: Fragment? = null
        try {
            fragment = fragmentClass.newInstance() as Fragment
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Insert the fragment by replacing any existing fragment
        val fragmentManager = fragmentActivity.supportFragmentManager
        fragmentManager.beginTransaction()
            .replace(R.id.container, fragment!!)
            .commit()
    }

    /**
     * Checks for specified permissions in fragment and asks for them if they haven't been acquired
     * yet.
     * @param [fragment] currently active fragment that needs the permissions
     * @param [permissions] list of needed permissions
     * @param [code] request code which will be returned in method onRequestPermissionsResult
     * @param [runnable] runnable that is run in case all permissions have already been acquired
     * @author blarc
     */
    fun checkPermissionFragment(fragment: Fragment, permissions: Array<String>, code: Int, runnable: Runnable) {

        val permissionsNeeded = mutableListOf<String>()
        val permissionsRationale = mutableListOf<String>()

        for (permission in permissions) {
            if (fragment.requireActivity().checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission)
                if (fragment.shouldShowRequestPermissionRationale(permission)) {
                    permissionsRationale.add(permission)
                }
            }
        }

        when {
            permissionsRationale.isNotEmpty() -> {
                openPermissionDialog(fragment, permissionsRationale.toTypedArray(), code)
            }
            permissionsNeeded.isNotEmpty() -> {
                fragment.requestPermissions(permissionsNeeded.toTypedArray(), code)
            }
            else -> {
                runnable.run()
            }
        }
    }

    /**
     * Function creates a dialog that opens when function shouldShowRequestPermissionRationale
     * returns true.
     * @param [fragment] currently active fragment that needs the permissions
     * @param [permissions] list of needed permissions
     * @param [code] request code which will be returned in method onRequestPermissionsResult
     * @author blarc
     */
    private fun openPermissionDialog(fragment: Fragment, permissions: Array<String>, code: Int) {
        val alertBuilder = AlertDialog.Builder(fragment.requireContext())
        alertBuilder.setCancelable(true)
        alertBuilder.setTitle("Permission is necessary")
        alertBuilder.setMessage("Permission is necessary")
        alertBuilder.setPositiveButton("Yes") { _, _ ->
            fragment.requestPermissions(permissions, code)
        }
    }
}