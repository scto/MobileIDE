package com.mobileide.core.pluginframework.sandbox-agent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.matcher.ElementMatchers.*
import java.lang.instrument.Instrumentation

object FileAccessAdvice {
    @Advice.OnMethodEnter
    @Throws(SecurityException::class)
    fun onEnter() {
        throw SecurityException("File system access via java.io.File is forbidden!")
    }
}

object SandboxAgent {
    @JvmStatic
    fun premain(agentArgs: String?, inst: Instrumentation) {
        println("🚀 Sandbox Agent aktiviert: Dateisystemzugriff wird überwacht.")
        AgentBuilder.Default()
            .type(named("java.io.File"))
            .transform { builder, _, _, _, _ ->
                builder.constructor(any()).intercept(Advice.to(FileAccessAdvice::class.java))
            }
            .installOn(inst)
    }
}
