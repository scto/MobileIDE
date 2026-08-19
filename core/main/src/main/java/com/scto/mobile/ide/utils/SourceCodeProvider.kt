package com.scto.mobile.ide.utils





import java.net.URL











enum class SourceCodeProvider(val drawableRes: Int, val viewStringRes: Int) {
    GitHub(com.scto.mobile.ide.core.main.R.drawable.github, com.scto.mobile.ide.core.main.R.string.view_github),
    GitLab(com.scto.mobile.ide.core.main.R.drawable.gitlab, com.scto.mobile.ide.core.main.R.string.view_gitlab),
    BitBucket(com.scto.mobile.ide.core.main.R.drawable.bitbucket, com.scto.mobile.ide.core.main.R.string.view_bitbucket),
    Other(com.scto.mobile.ide.core.main.R.drawable.xml, com.scto.mobile.ide.core.main.R.string.view_repo);

    companion object {
        fun fromUrl(url: String): SourceCodeProvider {
            val hostName = URL(url).host
            return when (hostName) {
                "github.com" -> GitHub
                "gitlab.com" -> GitLab
                "bitbucket.org" -> BitBucket
                else -> Other
            }
        }
    }
}
