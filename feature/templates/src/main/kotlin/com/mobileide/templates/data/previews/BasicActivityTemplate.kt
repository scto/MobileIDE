package com.mobileide.templates.data.previews

import com.mobileide.templates.model.Template
import com.mobileide.templates.model.TemplateFile

fun getBasicActivityTemplate(): Template {
    return Template(
        id = "basic_activity",
        name = "Basic Views Activity",
        description = "Creates a new project with a single activity, a Floating Action Button, and a menu.",
        category = "Activity (Views)",
        files = listOf(
            // Hier würden die entsprechenden Template-Dateien für eine Basic Activity mit XML-Layouts folgen
             TemplateFile(
                path = "app/src/main/res/layout/activity_main.xml",
                content = """
                    <!-- Beispiel-Layout für eine Basic Activity -->
                    <androidx.coordinatorlayout.widget.CoordinatorLayout
                        xmlns:android="http://schemas.android.com/apk/res/android"
                        xmlns:app="http://schemas.android.com/apk/res-auto"
                        xmlns:tools="http://schemas.android.com/tools"
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        tools:context=".MainActivity">

                        <com.google.android.material.appbar.AppBarLayout
                            android:layout_height="wrap_content"
                            android:layout_width="match_parent"
                            android:theme="@style/Theme.${'$'}{projectName}.AppBarOverlay">

                            <androidx.appcompat.widget.Toolbar
                                android:id="@+id/toolbar"
                                android:layout_width="match_parent"
                                android:layout_height="?attr/actionBarSize"
                                android:background="?attr/colorPrimary"
                                app:popupTheme="@style/Theme.${'$'}{projectName}.PopupOverlay" />

                        </com.google.android.material.appbar.AppBarLayout>

                        <include layout="@layout/content_main" />

                        <com.google.android.material.floatingactionbutton.FloatingActionButton
                            android:id="@+id/fab"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_gravity="bottom|end"
                            android:layout_marginEnd="@dimen/fab_margin"
                            android:layout_marginBottom="16dp"
                            app:srcCompat="@android:drawable/ic_dialog_email" />

                    </androidx.coordinatorlayout.widget.CoordinatorLayout>
                """.trimIndent()
            )
        )
    )
}
