package com.puboot.module.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.puboot.common.annotation.Cache;
import com.puboot.common.util.*;
import com.puboot.enums.SysConfigKey;
import com.puboot.exception.UploadFileNotFoundException;
import com.puboot.module.admin.service.OssService;
import com.puboot.module.admin.service.SysConfigService;
import com.puboot.module.admin.vo.CloudStorageConfigVo;
import com.puboot.module.admin.vo.UploadResponse;
import com.puboot.module.admin.vo.base.ResponseVo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Created with IntelliJ IDEA.
 *
 * @author linzhaoguan
 * @date 2020/3/31 2:41 下午
 */
@Slf4j
@Service
public class OssServiceImpl implements OssService {

    @Autowired
    protected SysConfigService sysConfigService;

    @Override
    @SneakyThrows
    public UploadResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UploadFileNotFoundException(UploadResponse.ErrorEnum.FILE_NOT_FOUND.msg);
        }
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        String value = sysConfigService.selectAll().get(SysConfigKey.CLOUD_STORAGE_CONFIG.getValue());
        CloudStorageConfigVo cloudStorageConfig = JSON.parseObject(value, CloudStorageConfigVo.class);

        String md5 = MD5.getMessageDigest(file.getBytes());
        String dir;
        String filePath = null;
        String domain = null;
        ResponseVo<?> responseVo;
        switch (cloudStorageConfig.getType()) {
            case CoreConst.UPLOAD_TYPE_QINIUYUN:
                domain = cloudStorageConfig.getQiniuDomain();
                dir = cloudStorageConfig.getQiniuPrefix();
                filePath = String.format("%1$s/%2$s%3$s", dir, md5, suffix);
                responseVo = QiNiuYunUtil.uploadFile(cloudStorageConfig, filePath, file.getBytes());
                break;
            case CoreConst.UPLOAD_TYPE_ALIYUN:
                domain = cloudStorageConfig.getAliyunDomain();
                dir = cloudStorageConfig.getAliyunPrefix();
                filePath = String.format("%1$s/%2$s%3$s", dir, md5, suffix);
                responseVo = AliYunUtil.uploadFile(cloudStorageConfig, filePath, file.getBytes());
                break;
            default:
                responseVo = ResultUtil.error("未配置云存储类型");
        }
        String url = String.format("%1$s/%2$s", domain, filePath);
        if (responseVo.getStatus().equals(CoreConst.SUCCESS_CODE)) {
            return UploadResponse.success(url, originalFilename, suffix, url, CoreConst.SUCCESS_CODE);
        } else {
            return UploadResponse.failed(originalFilename, CoreConst.FAIL_CODE, responseVo.getMsg());
        }
    }

    @Override
    @Cache
    public CloudStorageConfigVo getOssConfig() {
        String value = sysConfigService.selectAll().get(SysConfigKey.CLOUD_STORAGE_CONFIG.getValue());
        return JSON.parseObject(value, CloudStorageConfigVo.class);
    }

    @Override
    @Cache(flush = true)
    public boolean updateOssConfig(CloudStorageConfigVo cloudStorageConfig) {
        String value = JSON.toJSONString(cloudStorageConfig);
        return sysConfigService.updateByKey(SysConfigKey.CLOUD_STORAGE_CONFIG.getValue(), value);
    }

    @Override
    public int getOssConfigType() {
        return getOssConfig().getType();
    }


}
