package projects

import common.Branch
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import model.CIBuildModel
import model.JsonBasedGradleSubprojectProvider
import model.StatisticBasedFunctionalTestBucketProvider
import promotion.PromotionProject
import util.UtilPerformanceProject
import util.UtilProject
import java.io.File

class GradleBuildToolRootProject(branch: Branch) : Project({
    id = DslContext.projectId
    name = DslContext.projectName

    val model = CIBuildModel(
        projectId = "${DslContext.parentProjectId}_${branch.buildTypeId}",
        branch = branch,
        buildScanTags = listOf("Check"),
        subprojects = JsonBasedGradleSubprojectProvider(File("./subprojects.json"))
    )
    val gradleBuildBucketProvider = StatisticBasedFunctionalTestBucketProvider(model, File("./test-class-data.json"))
    subProject(CheckProject(model, gradleBuildBucketProvider))

    subProject(PromotionProject(model.branch))
    if (model.branch == Branch.Master) {
        subProject(UtilProject)
        subProject(UtilPerformanceProject)
    }
})
