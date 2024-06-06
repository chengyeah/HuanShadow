package com.tencent.shadow.core.gradle.extensions

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.tencent.shadow.core.gradle.ShadowPluginHelper
import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import java.io.File
import java.util.*

open class PackagePluginExtension {

    var loaderApkProjectPath = ""
    var runtimeApkProjectPath = ""

    var archivePrefix = ""
    var archiveSuffix = ""
    var destinationDir = ""

    var uuid = ""
    var version: Int = 0
    var uuidNickName = ""
    var compactVersion: Array<Int> = emptyArray()

    var buildTypes: NamedDomainObjectContainer<PluginBuildType>

    var channel = ""

    constructor(project: Project) {
        buildTypes = project.container(PluginBuildType::class.java)
        buildTypes.all {
            it.pluginApks = project.container(PluginApkConfig::class.java)
        }
    }

    fun pluginTypes(closure: Closure<PluginBuildType>) {
        buildTypes.configure(closure)
    }

    fun toJson(
        project: Project,
        loaderApkName: String,
        runtimeApkName: String,
        buildType: PluginBuildType
    ): JSONObject {
        val json = JSONObject()

        if (loaderApkName.isNotEmpty()) {
            //Json文件中 plugin-loader部分信息
            val pluginLoaderObj = JSONObject()
            pluginLoaderObj["apkName"] = loaderApkName
            val loaderFile = ShadowPluginHelper.getLoaderApkFile(project, buildType, true)
            pluginLoaderObj["hash"] = ShadowPluginHelper.getFileMD5(loaderFile)
            json["pluginLoader"] = pluginLoaderObj
        }


        if (runtimeApkName.isNotEmpty()) {
            //Json文件中 plugin-runtime部分信息
            val runtimeObj = JSONObject()
            runtimeObj["apkName"] = runtimeApkName
            val runtimeFile = ShadowPluginHelper.getRuntimeApkFile(project, buildType, true)
            runtimeObj["hash"] = ShadowPluginHelper.getFileMD5(runtimeFile)
            json["runtime"] = runtimeObj
        }


        //Json文件中 plugin部分信息
        val jsonArr = JSONArray()
        for (i in buildType.pluginApks) {
            val pluginObj = JSONObject()
            pluginObj["businessName"] = i.businessName
            pluginObj["partKey"] = i.partKey
            pluginObj["apkName"] = File(i.apkPath).name
            pluginObj["hash"] =
                ShadowPluginHelper.getFileMD5(ShadowPluginHelper.getPluginFile(project, i, true))
            if (i.dependsOn.isNotEmpty()) {
                val dependsOnJson = JSONArray()
                for (k in i.dependsOn) {
                    dependsOnJson.add(k)
                }
                pluginObj["dependsOn"] = dependsOnJson
            }
            if (i.hostWhiteList.isNotEmpty()) {
                val hostWhiteListJson = JSONArray()
                for (k in i.hostWhiteList) {
                    hostWhiteListJson.add(k)
                }
                pluginObj["hostWhiteList"] = hostWhiteListJson
            }
            jsonArr.add(pluginObj)
        }
        json["plugins"] = jsonArr


        //Config.json版本号
        if (version > 0) {
            json["version"] = version
        } else {
            json["version"] = 1
        }


        //uuid UUID_NickName
        val uuid = "${project.rootDir}" + "/build/uuid.txt"
        val uuidFile = File(uuid)
        when {
            uuidFile.exists() -> {
                json["UUID"] = uuidFile.readText()
                project.logger.info("uuid = " + json["UUID"] + " 由文件生成")
            }

            this.uuid.isEmpty() -> {
                json["UUID"] = UUID.randomUUID().toString().toUpperCase()
                project.logger.info("uuid = " + json["UUID"] + " 随机生成")
            }

            else -> {
                json["UUID"] = this.uuid
                project.logger.info("uuid = " + json["UUID"] + " 由配置生成")
            }
        }

        if (uuidNickName.isNotEmpty()) {
            json["UUID_NickName"] = uuidNickName
        } else {
            json["UUID_NickName"] = "1.0"
        }

        if (compactVersion.isNotEmpty()) {
            val jsonArray = JSONArray()
            for (i in compactVersion) {
                jsonArray.add(i)
            }
            json["compact_version"] = jsonArray
        }

        addCustomPrams(project, json)

        return json
    }

    private fun addCustomPrams(project: Project, json: JSONObject) {
        val android = project.extensions.findByName("android") as BaseAppModuleExtension
        val additionalParameters = android.aaptOptions.additionalParameters

        val pluginPackage = android.productFlavors.getByName("plugin").applicationId
        val pluginResId =
            if (additionalParameters.contains("--package-id")) additionalParameters.let {
                additionalParameters[additionalParameters.indexOf("--package-id") + 1]
            } else "0x7F"
        val customFlag = project.extensions.extraProperties.get("PLUGIN_FLAG")
        json["huan"] = JSONObject().apply {
            put("partKey", ((json["plugins"] as JSONArray)[0] as JSONObject)["partKey"])
            put("hostPkg", pluginPackage)
            put("uuid", json["UUID"])
            put("resId", pluginResId)
            put("vern", json["UUID_NickName"])
            put("verc", json["version"])
            put("channel", channel)
            put("flag", customFlag)
        }

    }
}