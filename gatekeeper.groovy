job('save-youtube-music') {
  parameters {
    stringParam('VIDEO_URL')
    stringParam('VIDEO_DEST', '', '/minivan/media/plex/youtube')
    stringParam('AUDIO_DEST', '/minivan/media/plex/music', '/minivan/media/plex/music\n/minivan/media/plex/audio')
    stringParam('TITLE')
    stringParam('ALBUM')
    stringParam('ARTIST')
  }
  scm {
    github('albinouty/youtube_audio_ripper')
  }
  steps {
    shell('rm -f youtube_*')
    shell('youtube-dl -o "youtube_%(title)s" $VIDEO_URL')
    shell("./extract_audio_from_video.cljs ./")
  }
}
