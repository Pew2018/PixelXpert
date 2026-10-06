plugins { alias(libs.plugins.android.application) }

android {
	namespace = "dev.pew2018.pixelcloneprofilehelper"
	compileSdk = 37
	defaultConfig {
		applicationId = "dev.pew2018.pixelcloneprofilehelper"
		minSdk = 29
		targetSdk = 35
		versionCode = 1
		versionName = "0.1"
	}
	buildTypes {
		debug { isMinifyEnabled = false }
		release {
			isMinifyEnabled = true
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
}
dependencies {
	implementation("com.google.android.material:material:1.12.0")
	implementation("androidx.appcompat:appcompat:1.7.1")
	compileOnly("io.github.libxposed:api:102.0.0")
	implementation("io.github.libxposed:service:102.0.0")
	testImplementation("junit:junit:4.13.2")
}
