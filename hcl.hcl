local {
  config {
    min_go_version = "1.21"
  }
}

run {
  env = {
    ANDROID_SDK_ROOT = "${get_env("ANDROID_SDK_ROOT", "$HOME/Android/sdk")}"
    ANDROID_NDK_ROOT = "${get_env("ANDROID_NDK_ROOT", "$HOME/Android/ndk/26.0.10792818")}"
  }
}

generate {
  skip = false
}
