# 地图骨架屏
土地管理管理相关部门一般会有地图开发的相关业务，开屏一般会展示某个区域的全局图，如果加载过慢的话，可以考虑添加一个骨架屏来提升体验，特别是一些需要离线加载的场景，在线场景可以安卓协同，或者使用web开发一套。
此项目通过地图边界数据来绘制骨架，提供了两种动画，线条动画如下，另一种为透明度的闪烁
![image.png](https://cdn.nlark.com/yuque/0/2023/png/504550/1700449394759-3b76ead1-4a30-4e10-8ffd-dce84fafc2ae.png#averageHue=%23e7e6e9&clientId=uf40f41d2-b7aa-4&from=paste&height=624&id=uc9373c82&originHeight=1248&originWidth=704&originalType=binary&ratio=2&rotation=0&showTitle=false&size=87595&status=done&style=none&taskId=u977654ea-0f7b-48d1-a448-44526cf9197&title=&width=352)
## 数据来源
从地图平台上下载或者是动态请求，如[高德地图](https://lbs.amap.com/api/webservice/guide/api/district/#instructions)(底部有测试功能，可直接获取)，天地图也有对应的接口，但是调用发现没有数据
## 使用
添加仓库
```groovy
// 添加仓库
dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url 'https://jitpack.io' }
		}
	}
// 添加依赖
dependencies {
	        implementation 'com.github.wangyou1024:skeleton:Tag'
}
```
xml调用
```xml
<com.wangyou.skeleton.map.MapSkeleton
        android:id="@+id/mapSkeleton"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>
```
默认参数，或者在xml中设置`app:porpertyName`，`map_skeleton_city_source`可以是省级拼音，或者`assets`下的文件名，`map_skeleton_animation_type`可以设置为`line`或者`alpha`
```xml
<style name="DefaultMapSkeleton">
        <item name="map_skeleton_background">#f2f3f5</item>
        <item name="map_skeleton_stroke_color">#BABABA</item>
        <item name="map_skeleton_stroke_width">2dp</item>
        <item name="map_skeleton_duration">600</item>
        <item name="map_skeleton_city_source">china_chongqing_liangping.txt</item>
        <item name="map_skeleton_city_name">梁平</item>
        <item name="map_skeleton_city_name_color">#BABABA</item>
        <item name="map_skeleton_city_name_size">25dp</item>
        <item name="map_skeleton_animation_type">line</item>
        <item name="map_skeleton_light_angle">30</item>
        <item name="map_skeleton_light_color">#FFFFFFFF</item>
        <item name="map_skeleton_alpha_max">1</item>
        <item name="map_skeleton_alpha_min">0.4</item>
</style>
```


