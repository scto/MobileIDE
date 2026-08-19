package com.scto.mobile.ide.extension.api
















/**
 * Marks a recommended entry point for extensions.
 *
 * APIs annotated with this annotation are intended to be called by extensions and are considered stable integration
 * points. Extension authors should prefer these APIs over internal methods when interacting with the app.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.SOURCE)
annotation class XedExtensionPoint
