package com.mobileide.templates.data.previews

import com.mobileide.templates.model.Template
import com.mobileide.templates.model.TemplateFile

fun getFragmentViewModelTemplate(): Template {
    return Template(
        id = "fragment_viewmodel",
        name = "Fragment + ViewModel",
        description = "Creates a new Fragment, its corresponding ViewModel, and a layout file.",
        category = "Fragment (Views)",
        files = listOf(
            TemplateFile(
                path = "app/src/main/kotlin/${'$'}{packagePath}/ui/main/MainFragment.kt",
                content = """
                    package ${'$'}{packageName}.ui.main

                    import androidx.lifecycle.ViewModelProvider
                    import android.os.Bundle
                    import androidx.fragment.app.Fragment
                    import android.view.LayoutInflater
                    import android.view.View
                    import android.view.ViewGroup
                    import ${'$'}{packageName}.R

                    class MainFragment : Fragment() {

                        companion object {
                            fun newInstance() = MainFragment()
                        }

                        private lateinit var viewModel: MainViewModel

                        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                                  savedInstanceState: Bundle?): View {
                            return inflater.inflate(R.layout.fragment_main, container, false)
                        }

                        override fun onActivityCreated(savedInstanceState: Bundle?) {
                            super.onActivityCreated(savedInstanceState)
                            viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
                            // TODO: Use the ViewModel
                        }
                    }
                """.trimIndent()
            ),
            TemplateFile(
                path = "app/src/main/kotlin/${'$'}{packagePath}/ui/main/MainViewModel.kt",
                content = """
                    package ${'$'}{packageName}.ui.main

                    import androidx.lifecycle.ViewModel

                    class MainViewModel : ViewModel() {
                        // TODO: Implement the ViewModel
                    }
                """.trimIndent()
            ),
            TemplateFile(
                path = "app/src/main/res/layout/fragment_main.xml",
                content = """
                    <?xml version="1.0" encoding="utf-8"?>
                    <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
                        xmlns:app="http://schemas.android.com/apk/res-auto"
                        xmlns:tools="http://schemas.android.com/tools"
                        android:id="@+id/main"
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        tools:context=".ui.main.MainFragment">

                        <TextView
                            android:id="@+id/message"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="MainFragment"
                            app:layout_constraintBottom_toBottomOf="parent"
                            app:layout_constraintEnd_toEndOf="parent"
                            app:layout_constraintStart_toStartOf="parent"
                            app:layout_constraintTop_toTopOf="parent" />

                    </androidx.constraintlayout.widget.ConstraintLayout>
                """.trimIndent()
            )
        )
    )
}
