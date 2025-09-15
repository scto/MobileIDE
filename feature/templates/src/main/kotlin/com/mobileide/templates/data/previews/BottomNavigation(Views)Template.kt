package com.mobileide.templates.data.previews

import com.mobileide.templates.model.Template
import com.mobileide.templates.model.TemplateFile

fun getBottomNavigationViewsTemplate(): Template {
    return Template(
        id = "bottom_nav_views",
        name = "Bottom Navigation Views Activity",
        description = "Creates an activity with a bottom navigation bar and multiple fragments.",
        category = "Activity (Views)",
        files = listOf(
            TemplateFile(
                path = "app/src/main/res/layout/activity_main.xml",
                content = """
                    <?xml version="1.0" encoding="utf-8"?>
                    <androidx.constraintlayout.widget.ConstraintLayout
                        xmlns:android="http://schemas.android.com/apk/res/android"
                        xmlns:app="http://schemas.android.com/apk/res-auto"
                        android:id="@+id/container"
                        android:layout_width="match_parent"
                        android:layout_height="match_parent">

                        <com.google.android.material.bottomnavigation.BottomNavigationView
                            android:id="@+id/nav_view"
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:background="?android:attr/windowBackground"
                            app:layout_constraintBottom_toBottomOf="parent"
                            app:layout_constraintLeft_toLeftOf="parent"
                            app:layout_constraintRight_toRightOf="parent"
                            app:menu="@menu/bottom_nav_menu" />

                        <fragment
                            android:id="@+id/nav_host_fragment_activity_main"
                            android:name="androidx.navigation.fragment.NavHostFragment"
                            android:layout_width="match_parent"
                            android:layout_height="match_parent"
                            app:defaultNavHost="true"
                            app:layout_constraintBottom_toTopOf="@id/nav_view"
                            app:layout_constraintLeft_toLeftOf="parent"
                            app:layout_constraintRight_toRightOf="parent"
                            app:layout_constraintTop_toTopOf="parent"
                            app:navGraph="@navigation/mobile_navigation" />

                    </androidx.constraintlayout.widget.ConstraintLayout>
                """.trimIndent()
            ),
            TemplateFile(
                path = "app/src/main/res/navigation/mobile_navigation.xml",
                content = """
                    <?xml version="1.0" encoding="utf-8"?>
                    <navigation xmlns:android="http://schemas.android.com/apk/res/android"
                        xmlns:app="http://schemas.android.com/apk/res-auto"
                        xmlns:tools="http://schemas.android.com/tools"
                        android:id="@+id/mobile_navigation"
                        app:startDestination="@+id/navigation_home">

                        <fragment
                            android:id="@+id/navigation_home"
                            android:name="${'$'}{packageName}.ui.home.HomeFragment"
                            android:label="@string/title_home"
                            tools:layout="@layout/fragment_home" />

                        <fragment
                            android:id="@+id/navigation_dashboard"
                            android:name="${'$'}{packageName}.ui.dashboard.DashboardFragment"
                            android:label="@string/title_dashboard"
                            tools:layout="@layout/fragment_dashboard" />

                    </navigation>
                """.trimIndent()
            ),
            TemplateFile(
                path = "app/src/main/res/menu/bottom_nav_menu.xml",
                content = """
                    <?xml version="1.0" encoding="utf-8"?>
                    <menu xmlns:android="http://schemas.android.com/apk/res/android">
                        <item
                            android:id="@+id/navigation_home"
                            android:icon="@drawable/ic_home_black_24dp"
                            android:title="@string/title_home" />
                        <item
                            android:id="@+id/navigation_dashboard"
                            android:icon="@drawable/ic_dashboard_black_24dp"
                            android:title="@string/title_dashboard" />
                    </menu>
                """.trimIndent()
            )
            // Plus die Kotlin-Dateien für MainActivity, die Fragments und ViewModels.
        )
    )
}
