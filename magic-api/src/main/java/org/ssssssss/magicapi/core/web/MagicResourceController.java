package org.ssssssss.magicapi.core.web;

import org.springframework.web.bind.annotation.*;
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.exception.InvalidArgumentException;
import org.ssssssss.magicapi.core.interceptor.Authorization;
import org.ssssssss.magicapi.core.model.*;
import org.ssssssss.magicapi.core.resource.FileResource;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.utils.IoUtils;
import org.ssssssss.magicapi.utils.ROT13Utils;
import org.ssssssss.magicapi.utils.XxlJobAdminClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

public class MagicResourceController extends MagicController implements MagicExceptionHandler {

	private final MagicResourceService service;
    private final XxlJobAdminClient xxlJobAdminClient;   // 新增

	public MagicResourceController(MagicConfiguration configuration,
                                   XxlJobAdminClient xxlJobAdminClient) {
		super(configuration);
		this.service = MagicConfiguration.getMagicResourceService();
        this.xxlJobAdminClient = xxlJobAdminClient;
	}

	@PostMapping("/resource/folder/save")
	@ResponseBody
	public JsonBean<String> saveFolder(@RequestBody Group group, MagicHttpServletRequest request) {
		isTrue(allowVisit(request, Authorization.SAVE, group), PERMISSION_INVALID);
		Resource resource = service.getResource();
		if(resource instanceof FileResource){
			isTrue(resource.exists(), FILE_PATH_NOT_EXISTS);
		}
		if (service.saveGroup(group)) {
			return new JsonBean<>(group.getId());
		}
		return new JsonBean<>((String) null);
	}

	@PostMapping("/resource/folder/copy")
	@ResponseBody
	public JsonBean<String> saveFolder(String src, String target, MagicHttpServletRequest request) {
		Group srcGroup = service.getGroup(src);
		notNull(srcGroup, GROUP_NOT_FOUND);
		isTrue(allowVisit(request, Authorization.VIEW, srcGroup), PERMISSION_INVALID);
		Group targetGroup = srcGroup.copy();
		targetGroup.setId(null);
		targetGroup.setParentId(target);
		targetGroup.setType(srcGroup.getType());
		isTrue(allowVisit(request, Authorization.SAVE, targetGroup), PERMISSION_INVALID);
		return new JsonBean<>(service.copyGroup(src, target));
	}

	@PostMapping("/resource/delete")
	@ResponseBody
	public JsonBean<Boolean> delete(String id, MagicHttpServletRequest request) {
		Group group = service.getGroup(id);
		if(group == null){
			MagicEntity entity = service.file(id);
			notNull(entity, FILE_NOT_FOUND);
			isTrue(allowVisit(request, Authorization.DELETE, entity), PERMISSION_INVALID);
		} else {
			isTrue(allowVisit(request, Authorization.DELETE, group), PERMISSION_INVALID);
		}
		return new JsonBean<>(service.delete(id));
	}

	@PostMapping("/resource/file/{folder}/save")
	@ResponseBody
	public JsonBean<String> saveFile(@PathVariable("folder") String folder, String auto, MagicHttpServletRequest request) throws IOException {
		byte[] bytes = IoUtils.bytes(request.getInputStream());
		String encrypt = new String(bytes, StandardCharsets.UTF_8);
		String decrypt = ROT13Utils.decrypt(encrypt);
		MagicEntity entity = configuration.getMagicDynamicRegistries().stream()
				.map(MagicDynamicRegistry::getMagicResourceStorage)
				.filter(it -> Objects.equals(it.folder(), folder))
				.findFirst()
				.orElseThrow(() -> new InvalidArgumentException(GROUP_NOT_FOUND))
				.read(decrypt.getBytes(StandardCharsets.UTF_8));
		isTrue(allowVisit(request, Authorization.SAVE, entity), PERMISSION_INVALID);
		// 自动保存的代码，和旧版代码对比，如果一致，则不保存，直接返回。

        // ===== 抽取 entity 的元素调用 xxl-job 保存 =====

//        Map<String, Object> jobInfo = new HashMap<>();
//
//// 必填字段
//        jobInfo.put("jobGroup", 1);                         // 执行器ID（根据你xxl-job-admin里的执行器配置）
//        jobInfo.put("jobDesc", entity.getScript());         // 任务描述
//        jobInfo.put("author", "hoshea");                   // 负责人
//        jobInfo.put("scheduleType", "CRON");               // 调度类型
//        jobInfo.put("scheduleConf", ((TaskInfo) entity).getCron());   // cron 表达式
//
//        jobInfo.put("glueType", "BEAN");                   // GLUE类型
//        jobInfo.put("executorHandler", "magicJobHandler");// 执行器Handler名
//        jobInfo.put("glueSource", entity.getScript());     // 脚本内容
//
//        jobInfo.put("triggerStatus",
//                ((TaskInfo) entity).isEnabled() ? "1" : "0"); // 是否启用
//
//// 可选字段（建议补全）
//        jobInfo.put("executorParam", "");
//        jobInfo.put("executorRouteStrategy", "FIRST");
//        jobInfo.put("executorBlockStrategy", "SERIAL_EXECUTION");
//        jobInfo.put("executorTimeout", 0);
//        jobInfo.put("executorFailRetryCount", 0);
//        jobInfo.put("misfireStrategy", "DO_NOTHING");
//        jobInfo.put("glueRemark", "GLUE代码初始化");
//        jobInfo.put("childJobId", "");
//        jobInfo.put("alarmEmail", "3093652682@qq.com");
//
//// ===== 调用 xxl-job-admin 接口 =====
//        String result = xxlJobAdminClient.insertJob(jobInfo);
//        logger.info("调用 xxl-job 插入结果: {}", result);

        //判断是api还是task还是function
        if (entity instanceof MagicEntity  && folder.equals("task")) {
            logger.info("保存的是任务，因此进入任务保存到xxl-job");
            Object cron = null;
            Object enabled = null;
            try {
                cron = entity.getClass().getMethod("getCron").invoke(entity);
                enabled = entity.getClass().getMethod("isEnabled").invoke(entity);
            } catch (NoSuchMethodException ignored) {
                // 没有 getCron 方法，则跳过
            } catch (Exception e) {
                logger.error("读取 cron/enabled 出错", e);
            }


            if (cron != null) {
                Map<String, Object> jobInfo = new HashMap<>();
//                jobInfo.put("id", 45);
                jobInfo.put("jobGroup", 1);
                jobInfo.put("jobDesc", entity.getName());
                jobInfo.put("author", "hoshea");
                jobInfo.put("scheduleType", "CRON");
                jobInfo.put("scheduleConf", cron.toString());

                jobInfo.put("glueType", "BEAN");
                jobInfo.put("executorHandler", "magicJobHandler");
                jobInfo.put("glueSource", entity.getScript());

                jobInfo.put("triggerStatus",
                        "true".equals(String.valueOf(enabled)) ? "1" : "0");
                        jobInfo.put("executorParam", "");
                jobInfo.put("executorRouteStrategy", "FIRST");
                jobInfo.put("executorBlockStrategy", "SERIAL_EXECUTION");
                jobInfo.put("executorTimeout", 0);
                jobInfo.put("executorFailRetryCount", 0);
                jobInfo.put("misfireStrategy", "DO_NOTHING");
                jobInfo.put("glueRemark", "GLUE代码初始化");
                jobInfo.put("childJobId", "");
                jobInfo.put("alarmEmail", "3093652682@qq.com");

//                String result = xxlJobAdminClient.insertJob(jobInfo);
                String result;

                    result = xxlJobAdminClient.updateJob(jobInfo);
                    logger.info("XXL-JOB 更新结果: {}", result);

//                    result = xxlJobAdminClient.insertJob(jobInfo);
//                    logger.info("XXL-JOB 插入结果: {}", result);
//                }

//                logger.info("XXL-JOB 插入结果: {}", result);
            }
        }


        if (entity ==null){
			return new JsonBean<>(null);
		}
		if(entity.getId() != null && "1".equals(auto)){
			MagicEntity oldInfo = service.file(entity.getId());
			if(oldInfo != null && Objects.equals(oldInfo, entity)){
				return new JsonBean<>(entity.getId());
			}
		}
		if (MagicConfiguration.getMagicResourceService().saveFile(entity)) {
			return new JsonBean<>(entity.getId());
		}
		return new JsonBean<>(null);
	}

	@GetMapping("/resource/file/{id}")
	@ResponseBody
	public JsonBean<MagicEntity> detail(@PathVariable("id") String id, MagicHttpServletRequest request) {
		MagicEntity entity = MagicConfiguration.getMagicResourceService().file(id);
		isTrue(allowVisit(request, Authorization.VIEW, entity), PERMISSION_INVALID);
		return new JsonBean<>(entity);
	}

	@PostMapping("/resource/move")
	@ResponseBody
	public JsonBean<Boolean> move(String src, String groupId, MagicHttpServletRequest request) {
		Group group = service.getGroup(src);
		if(group == null){
			MagicEntity entity = service.file(src);
			notNull(entity, FILE_NOT_FOUND);
			entity = entity.copy();
			entity.setGroupId(groupId);
			isTrue(allowVisit(request, Authorization.SAVE, entity), PERMISSION_INVALID);
		} else {
			group = group.copy();
			group.setParentId(groupId);
			isTrue(allowVisit(request, Authorization.DELETE, group), PERMISSION_INVALID);
		}
		return new JsonBean<>(service.move(src, groupId));
	}

	@PostMapping("/resource/lock")
	@ResponseBody
	public JsonBean<Boolean> lock(String id, MagicHttpServletRequest request) {
		MagicEntity entity = service.file(id);
		notNull(entity, FILE_NOT_FOUND);
		isTrue(allowVisit(request, Authorization.LOCK, entity), PERMISSION_INVALID);
		return new JsonBean<>(service.lock(id));
	}

	@PostMapping("/resource/unlock")
	@ResponseBody
	public JsonBean<Boolean> unlock(String id, MagicHttpServletRequest request) {
		MagicEntity entity = service.file(id);
		notNull(entity, FILE_NOT_FOUND);
		isTrue(allowVisit(request, Authorization.UNLOCK, entity), PERMISSION_INVALID);
		return new JsonBean<>(service.unlock(id));
	}

	@PostMapping("/resource")
	@ResponseBody
	public JsonBean<Map<String, TreeNode<Attributes<Object>>>> resources(MagicHttpServletRequest request) {
		Map<String, TreeNode<Group>> tree = service.tree();
		Map<String, TreeNode<Attributes<Object>>> result = new HashMap<>();
		tree.forEach((key, value) -> {
			TreeNode<Attributes<Object>> node = process(value, request);
			List<TreeNode<Attributes<Object>>> groups = node.getChildren();
			if(groups.size() > 0){
				List<TreeNode<Attributes<Object>>> nodes = groups.get(0).getChildren();
				configuration.getMagicDynamicRegistries().stream()
						.filter(it -> it.getMagicResourceStorage().folder().equals(key))
						.findFirst()
						.map(MagicDynamicRegistry::defaultMappings)
						.ifPresent(mappings -> {
							for (MagicEntity mapping : mappings) {
								nodes.add(new TreeNode<>(mapping));
							}
						});
			}
			result.put(key, node);
		});
		return new JsonBean<>(result);
	}

	private TreeNode<Attributes<Object>> process(TreeNode<Group> groupNode, MagicHttpServletRequest request) {
		TreeNode<Attributes<Object>> value = new TreeNode<>();
		value.setNode(groupNode.getNode());
		groupNode.getChildren().stream()
				.filter(it -> allowVisit(request, Authorization.VIEW, it.getNode()))
				.map(it -> process(it, request))
				.forEach(value::addChild);
		if (!Constants.ROOT_ID.equals(groupNode.getNode().getId())) {
			service
					.listFiles(groupNode.getNode().getId())
					.stream()
					.filter(it -> allowVisit(request, Authorization.VIEW, it))
					.map(MagicEntity::simple)
					.sorted(Comparator.comparing(MagicEntity::getName))
					.map((Function<MagicEntity, TreeNode>) TreeNode::new)
					.forEach(value::addChild);
		}
		return value;
	}
}
