# StreamingMusicPlayerPlus
CyberAgentのインターン中に作ったストリーミング配信音楽プレイヤー


![gif](https://github.com/Hiroki11x/TestAWAPlayer/blob/master/movie.gif)

## OverView
ストリーミング音楽配信サービスであるAWAの現状の課題を改善したAndroidアプリの開発を一から一週間で行いました。
担当したのは、iThuneAPIで音楽情報・音源取得の実装、プレイヤー画面の実装(基本的な再生、次の曲、早送り、UI全て)、バックグラウンド処理、Notificationでの操作の実装・全体的な画面遷移などの設計、データベース・履歴・次再生曲リスト機能の実装、スタートの人気曲Glid表示画面の実装、検索機能の実装などを行いました。
(https://www.youtube.com/watch?v=fw7BfEbH_RA)


### Point
* AWAに近いようなUXを意識し、ObservableScrollViewを取り入れました
* UIにも気を配りアプリケーションの世界観を統一できるよう、細かい透明度の変化や画像の透かしなどを行いました
* 音楽プレーヤーとして最低限機能するようバックグラウンド処理・Notificationでの操作なども実装しました
* デザインをシンプルにすることで直感的な操作ができるようにしました


## Development technology
#### API・Data
* iTunesAPI


#### Framework / Library / Module
* Android Studio
* Android Annotations
* Butterknife
* Piccasso
* Recyclerview
* Observablescrollview
* Easyfonts
* Android Design Support Library
* Calligraphy
* SwipeLayout
* Materialmenu


#### デバイス
* Android


### Original Technology
#### Original function and technology developed
* 円形のSeekBar
* ObservableScrollViewを用いて細かい動きに対応したPlayer画面にした
* 独自のRevealエフェクトをかけられるライブラリ(自作)を実装し、画面遷移を滑らかにした(https://github.com/Hiroki11x/RevealEffectTransition)
