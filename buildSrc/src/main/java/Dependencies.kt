/**
implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.6.1"
implementation "androidx.lifecycle:lifecycle-livedata-ktx:2.6.1"
implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1"

implementation 'androidx.navigation:navigation-fragment-ktx:2.5.3'
implementation 'androidx.navigation:navigation-ui-ktx:2.5.3'
testImplementation 'junit:junit:4.13.2'
androidTestImplementation 'androidx.test.ext:junit:1.1.5'
androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
 */
object Dependencies {
    const val CORE_KTX = "androidx.core:core-ktx:${Versions.CORE_KTX}"
    const val APP_COMPAT = "androidx.appcompat:appcompat:${Versions.APP_COMPAT}"
    const val MATERIAL = "com.google.android.material:material:${Versions.MATERIAL}"
    const val CONSTRAINT_LAYOUT =
        "androidx.constraintlayout:constraintlayout:${Versions.CONSTRAINT_LAYOUT}"
    const val FRAGMENT_KTX = "androidx.fragment:fragment-ktx:${Versions.FRAGMENT_KTX}"

    const val DAGGER_HILT = "com.google.dagger:hilt-android:${Versions.HILT}"
    const val DAGGER_HILT_KAPT = "com.google.dagger:hilt-compiler:${Versions.HILT}"
    const val WORKMANAGER = "androidx.work:work-runtime-ktx:${Versions.WORKMANAGER}"

    const val NAVIGATION_FRAGMENT_KTX =
        "androidx.navigation:navigation-fragment-ktx:${Versions.NAVIGATION}"
    const val NAVIGATION_UI_KTX = "androidx.navigation:navigation-ui-ktx:${Versions.NAVIGATION}"
}