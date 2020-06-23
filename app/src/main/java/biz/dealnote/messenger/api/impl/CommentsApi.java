package biz.dealnote.messenger.api.impl;

import biz.dealnote.messenger.api.IServiceProvider;
import biz.dealnote.messenger.api.interfaces.ICommentsApi;
import biz.dealnote.messenger.api.model.response.CustomCommentsResponse;
import biz.dealnote.messenger.api.services.ICommentsService;
import io.reactivex.Single;


class CommentsApi extends AbsApi implements ICommentsApi {

    CommentsApi(int accountId, IServiceProvider provider) {
        super(accountId, provider);
    }

    @Override
    public Single<CustomCommentsResponse> get(String sourceType, int ownerId, int sourceId, Integer offset,
                                              Integer count, String sort, Integer startCommentId, Integer threadComment, String accessKey, String fields) {
        final int thread_id = threadComment != null ? threadComment : 0;
        return provideService(ICommentsService.class)
                .flatMap(service -> service
                        .get("var comment_id = Args.comment_id;\n" +
                                "var owner_id = Args.owner_id;\n" +
                                "var source_id = Args.source_id;\n" +
                                "var start_comment_id = Args.start_comment_id;\n" +
                                "var offset = Args.offset;\n" +
                                "var count = Args.count;\n" +
                                "var sort = Args.sort;\n" +
                                "var access_key = Args.access_key;\n" +
                                "var source_type = Args.source_type;\n" +
                                "var fields = Args.fields;\n" +
                                "\n" +
                                "var positive_group_id;\n" +
                                "if(owner_id < 0){\n" +
                                "    positive_group_id = -owner_id;\n" +
                                "} else {\n" +
                                "    positive_group_id = owner_id;\n" +
                                "}\n" +
                                "\n" +
                                "var admin_level = 0;\n" +
                                "if(owner_id < 0){\n" +
                                "    admin_level = API.groups.getById({\"group_id\":positive_group_id,\n" +
                                "        \"fields\":\"admin_level\"})[0].admin_level;\n" +
                                "}\n" +
                                "\n" +
                                "var result;\n" +
                                "if(source_type == \"post\"){\n" +
                                "    if(comment_id != 0)\n" +
                                "    {\n" +
                                "    \tresult = API.wall.getComments({\"owner_id\":owner_id, \n" +
                                "        \"post_id\":source_id, \n" +
                                "        \"need_likes\":\"1\", \n" +
                                "        \"start_comment_id\":start_comment_id, \n" +
                                "        \"offset\":offset, \n" +
                                "        \"count\":count, \n" +
                                "        \"sort\":sort, \n" +
                                "        \"preview_length\":\"0\", \n" +
                                "        \"extended\":\"1\",\n" +
                                "        \"comment_id\":comment_id,\n" +
                                "        \"thread_items_count\":\"10\",\n" +
                                "        \"fields\":fields\n" +
                                "    });\n" +
                                "    }\n" +
                                "    else\n" +
                                "    {\n" +
                                "    result = API.wall.getComments({\"owner_id\":owner_id, \n" +
                                "        \"post_id\":source_id, \n" +
                                "        \"need_likes\":\"1\", \n" +
                                "        \"start_comment_id\":start_comment_id, \n" +
                                "        \"offset\":offset, \n" +
                                "        \"count\":count, \n" +
                                "        \"sort\":sort, \n" +
                                "        \"preview_length\":\"0\", \n" +
                                "        \"extended\":\"1\",\n" +
                                "        \"thread_items_count\":\"10\",\n" +
                                "        \"fields\":fields\n" +
                                "    });\n" +
                                "    }\n" +
                                "}\n" +
                                "\n" +
                                "if(source_type == \"photo\"){\n" +
                                "    result = API.photos.getComments({\"owner_id\":owner_id, \n" +
                                "        \"photo_id\":source_id, \n" +
                                "        \"need_likes\":\"1\", \n" +
                                "        \"start_comment_id\":start_comment_id, \n" +
                                "        \"offset\":offset, \n" +
                                "        \"count\":count, \n" +
                                "        \"sort\":sort, \n" +
                                "        \"extended\":\"1\",\n" +
                                "        \"fields\":fields,\n" +
                                "        \"access_key\":access_key});\n" +
                                "}\n" +
                                "\n" +
                                "if(source_type == \"video\"){\n" +
                                "    result = API.video.getComments({\"owner_id\":owner_id, \n" +
                                "        \"video_id\":source_id, \n" +
                                "        \"need_likes\":\"1\", \n" +
                                "        \"start_comment_id\":start_comment_id, \n" +
                                "        \"offset\":offset, \n" +
                                "        \"count\":count, \n" +
                                "        \"sort\":sort, \n" +
                                "        \"extended\":\"1\",\n" +
                                "        \"fields\":fields\n" +
                                "    });\n" +
                                "}\n" +
                                "\n" +
                                "if(source_type == \"topic\"){\n" +
                                "    result = API.board.getComments({\"group_id\":positive_group_id, \n" +
                                "        \"topic_id\":source_id, \n" +
                                "        \"need_likes\":\"1\", \n" +
                                "        \"start_comment_id\":start_comment_id, \n" +
                                "        \"offset\":offset, \n" +
                                "        \"count\":count, \n" +
                                "        \"sort\":sort, \n" +
                                "        \"extended\":\"1\"});\n" +
                                "}\n" +
                                "\n" +
                                "var first_id;\n" +
                                "if(source_type == \"post\"){\n" +
                                "   if(comment_id != 0)\n" +
                                "   {\n" +
                                "   first_id = API.wall.getComments({\"owner_id\":owner_id, \n" +
                                "        \"post_id\":source_id, \"count\":\"1\", \"sort\":\"asc\", \"comment_id\":comment_id, \"thread_items_count\":\"10\"}).items[0].id;\n" +
                                "   }\n" +
                                "   else\n" +
                                "   {\n" +
                                "   first_id = API.wall.getComments({\"owner_id\":owner_id, \n" +
                                "        \"post_id\":source_id, \"count\":\"1\", \"sort\":\"asc\", \"thread_items_count\":\"10\"}).items[0].id;\n" +
                                "   }\n" +
                                "}\n" +
                                "\n" +
                                "if(source_type == \"photo\"){\n" +
                                "   first_id = API.photos.getComments({\n" +
                                "        \"owner_id\":owner_id, \n" +
                                "        \"photo_id\":source_id, \n" +
                                "        \"count\":\"1\", \n" +
                                "        \"sort\":\"asc\", \n" +
                                "        \"access_key\":access_key}).items[0].id;\n" +
                                "}\n" +
                                "\n" +
                                "if(source_type == \"video\"){\n" +
                                "   first_id = API.video.getComments({\"owner_id\":owner_id, \n" +
                                "        \"video_id\":source_id, \"count\":\"1\", \"sort\":\"asc\"}).items[0].id;\n" +
                                "}\n" +
                                "\n" +
                                "if(source_type == \"topic\"){\n" +
                                "    first_id = API.board.getComments({\"group_id\":positive_group_id, \n" +
                                "        \"topic_id\":source_id, \"count\":\"1\", \"sort\":\"asc\"}).items[0].id;\n" +
                                "}\n" +
                                "\n" +
                                "var last_id;\n" +
                                "if(source_type == \"post\"){\n" +
                                "    if(comment_id != 0)\n" +
                                "    {\n" +
                                "    last_id = API.wall.getComments({\"owner_id\":owner_id, \n" +
                                "        \"post_id\":source_id, \"count\":\"1\", \"sort\":\"desc\", \"comment_id\":comment_id, \"thread_items_count\":\"10\"}).items[0].id;\n" +
                                "    }\n" +
                                "    else\n" +
                                "    {\n" +
                                "    last_id = API.wall.getComments({\"owner_id\":owner_id, \n" +
                                "        \"post_id\":source_id, \"count\":\"1\", \"sort\":\"desc\", \"thread_items_count\":\"10\"}).items[0].id;\n" +
                                "    }\n" +
                                "}\n" +
                                "\n" +
                                "if(source_type == \"photo\"){\n" +
                                "    last_id = API.photos.getComments({\"owner_id\":owner_id, \n" +
                                "        \"photo_id\":source_id, \"count\":\"1\", \"sort\":\"desc\"}).items[0].id;\n" +
                                "}\n" +
                                "\n" +
                                "if(source_type == \"video\"){\n" +
                                "    last_id = API.video.getComments({\"owner_id\":owner_id, \n" +
                                "        \"video_id\":source_id, \"count\":\"1\", \"sort\":\"desc\"}).items[0].id;\n" +
                                "}\n" +
                                "\n" +
                                "if(source_type == \"topic\"){\n" +
                                "    last_id = API.board.getComments({\"group_id\":positive_group_id, \n" +
                                "        \"topic_id\":source_id, \"count\":\"1\", \"sort\":\"desc\"}).items[0].id;\n" +
                                "}\n" +
                                "\n" +
                                "return {\"main\": result, \n" +
                                "    \"first_id\": first_id, \n" +
                                "    \"last_id\": last_id, \n" +
                                "    \"admin_level\": admin_level};", sourceType, ownerId, sourceId, offset, count, sort, startCommentId, thread_id, accessKey, fields)
                        .map(handleExecuteErrors("execute.getComments"))
                        .map(extractResponseWithErrorHandling()));
    }
}