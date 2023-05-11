package com.phazei.dynamicgptchat.about

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.phazei.dynamicgptchat.R

class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val aboutText: TextView = view.findViewById(R.id.about_text)
        val htmlText = """
<h1>About DynamicGPTChat</h1>
<p>Welcome to <a href="https://github.com/phazei/dynamicGPTChat/">DynamicGPTChat</a>, a mobile app built for those who want a more enjoyable and tailored experience using ChatGPT. This app is designed to be open source, allowing anyone to build upon it and use it for free as long as they have an API key and comply with the terms of the <a href="https://github.com/phazei/dynamicGPTChat/blob/master/LICENSE.txt">DynamicGPTChat Software License</a>.</p>
<br /><br />
<h2>Privacy</h2>
<p>The app does not have a backend server, which means your API key is only sent directly to the OpenAI API and is never passed on to anyone else.</p>
<br /><br />
<h2>Attributions</h2>
<p>DynamicGPTChat uses the following open-source libraries:</p>
<ul>
  <li><a href="https://github.com/noties/Markwon">Markwon</a> (Apache 2.0)</li>
  <li><a href="https://github.com/aallam/openai-kotlin">openai-kotlin</a> (Apache 2.0)</li>
  <li><a href="https://github.com/tomergoldst/tooltips">Tooltips</a> (Apache 2.0)</li>
  <li><a href="https://github.com/pilgr/Paper">Paper</a> (Apache 2.0)</li>
  <li><a href="https://github.com/square/moshi">Moshi</a> (Apache 2.0)</li>
  <li><a href="Library URL">Library Name</a> (MIT)</li>
</ul>
<br /><br />
<p>We're grateful to the developers of these libraries for their incredible work and contributions to the open-source community.</p>"""
        aboutText.text = HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_COMPACT)
        aboutText.movementMethod = LinkMovementMethod.getInstance()

    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}