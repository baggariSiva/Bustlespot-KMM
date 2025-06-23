package org.softsuave.bustlespot.data.network


object APIEndpoints {
/*
    const val SIGNIN = "/api/auth/signin" // Node API
    const val SIGNOUT = "/api/auth/signout"
    const val GETALLORGANISATIONS ="/api/organisation/getUserOrganization"


    const val GETALLPROJECTS = "/api/project/getProjectList"

 */
    const val GETALLTASKS = "/api/task/getTaskByProjectId"

    const val POSTACTIVITY = "/api/activity/addActivityList"
    const val GETALLACTIVITIES = "/api/activity/get-all-activity"

    const val UPDATEACTIVITY = "/api/activity/updateActivity"


    // python api
    const val SIGNIN = "/auth/signin"
    const val GETALLORGANISATIONS ="/organisation/get_all_organisation"
    const val SIGNOUT = "/auth/signout"
    const val GETALLPROJECTS = "/project/getProjectList"

}