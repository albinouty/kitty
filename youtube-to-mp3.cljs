#!/usr/bin/env planck

(ns friday.youtube-to-mp3
  (:require [clojure.pprint :as pprint]
            [clojure.string :as string]
            [planck.core :as core]
            [planck.io :as io]
            [planck.shell :as sh]
            [planck.environ :as env]))


(defn prn-err
  [& args]
  (binding [*out* core/*err*]
    (apply prn args)))

(defmacro with-err
  [& body]
  `(binding [*out* core/*err*]
     ~@body))

(defn find-youtube-files
  [dir]
  (->> (sh/sh "ls" dir)
       (:out)
       (string/split-lines)
       (filter #(.startsWith % "youtube_"))))

(defn sh
  [& args]
  (let [{:keys [exit err] :as result} (apply sh/sh (map str args))]
    (when (not (zero? exit))
      (with-err 
        (println err)) 
      (core/exit exit))
    result))

(defn move-file
  [src dest]
  (sh "mv" src dest))

(defn vlc-extract-audio
  [in out]
  (let [transcode-opt "#transcode{acodec=mp3,ab=320,vcodec=dummy}"
        output-opt (str "std{access=file,mux=raw,dst=\"" out "\"}")
        opt (str transcode-opt ":" output-opt)
        cmd ["vlc" "-I" "dummy" in "--sout" opt "vlc://quit"]]
    (prn-err cmd)
    (apply sh cmd)))

(defn set-tag
  ([file tag env-var-name]
   (if (exists? env/env env-var-name)
     (prn-err (sh "id3v2" (str "--" (name tag)) (get env/env env-var-name) file))
     (prn-err "Did not add id3v2 tag" tag "," env-var-name "not found.")))
  ([file tag]
   (set-tag file tag tag)))

(defn -main
  [& args]
  (when (not= 1 (count args))
    (prn-err "Usage: youtube_to_mp3.cljs <directory>")
    (core/exit 1))
  (let [dir (first args)
        [f :as files] (find-youtube-files dir)]
    (when (empty? files)
      (prn-err "No youtube files found")
      (core/exit 0))
    (when (> (count files) 1)
      (prn-err "More than 1 youtube file found!")
      (core/exit 1))
    (when (exists? env/env :audio-dest)
      (prn-err env/env)
      (let [audio-dest (:audio-dest env/env)
            mp3-file (io/file audio-dest (str f ".mp3"))]
        (prn-err "Extracting audio...")
        (prn-err (vlc-extract-audio (io/file dir f) mp3-file))
        (prn-err "Done extracting audio...")
        (prn-err "Setting id3v2 tags")
        (set-tag mp3-file :album)
        (set-tag mp3-file :artist)
        (set-tag mp3-file :song :title)
        (prn-err "Done")))
    (when-let [video-dest (:video-dest env/env)]
      (prn-err "Moving video file...")
      (move-file (io/file dir f) (io/file video-dest f))
      (prn-err "Done moving video file."))))


(set! *main-cli-fn* -main)
