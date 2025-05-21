<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="参数名称" prop="configName">
        <el-input
          v-model="queryParams.configName"
          placeholder="请输入参数名称"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="参数键名" prop="configKey">
        <el-input
          v-model="queryParams.configKey"
          placeholder="请输入参数键名"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-refresh"
          size="mini"
          @click="handleRefreshCache"
          v-hasPermi="['system:ai:edit']"
        >刷新缓存</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-chat-dot-round"
          size="mini"
          @click="handleTestConnection"
          v-hasPermi="['system:ai:edit']"
        >测试连接</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="configList">
      <el-table-column label="参数名称" align="center" prop="configName" :show-overflow-tooltip="true" />
      <el-table-column label="参数键名" align="center" prop="configKey" :show-overflow-tooltip="true" />
      <el-table-column label="参数键值" align="center" prop="configValue" :show-overflow-tooltip="true" />
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:ai:edit']"
          >修改</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改参数配置对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="参数名称" prop="configName">
          <el-input v-model="form.configName" placeholder="请输入参数名称" />
        </el-form-item>
        <el-form-item label="参数键名" prop="configKey">
          <el-input v-model="form.configKey" placeholder="请输入参数键名" />
        </el-form-item>
        <el-form-item label="参数键值" prop="configValue">
          <el-input v-model="form.configValue" type="textarea" 
            :rows="form.configKey === 'ai.api.key' ? 3 : form.configKey === 'ai.system.prompt' ? 5 : 2" 
            placeholder="请输入参数键值" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 测试连接结果对话框 -->
    <el-dialog title="AI服务测试结果" :visible.sync="testResultVisible" width="500px" append-to-body>
      <div v-if="testLoading" style="text-align: center; padding: 20px;">
        <el-progress type="circle" :percentage="testProgress" :status="testStatus || undefined"></el-progress>
        <p style="margin-top: 10px;">{{ testMessage }}</p>
      </div>
      <div v-else>
        <div v-if="testSuccess" style="padding: 10px;">
          <el-alert
            title="连接成功"
            type="success"
            :closable="false"
            show-icon>
          </el-alert>
          <div style="margin-top: 20px; border: 1px solid #eee; padding: 10px; border-radius: 5px;">
            <p style="font-weight: bold;">AI回复:</p>
            <p>{{ testResponse }}</p>
          </div>
          <div style="margin-top: 10px; color: #666; font-size: 0.9em;">
            <p>使用模型: {{ testModel }}</p>
            <p>响应时间: {{ testResponseTime }}ms</p>
          </div>
        </div>
        <div v-else style="padding: 10px;">
          <el-alert
            title="连接失败"
            type="error"
            :closable="false"
            show-icon>
          </el-alert>
          <div style="margin-top: 20px; color: #666;">
            <p>错误信息: {{ testError }}</p>
          </div>
        </div>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="testResultVisible = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listConfig, getConfig, updateConfig, refreshCache } from "@/api/system/config";
import { sendMessageToAI } from "@/api/ai";

export default {
  name: "AIConfig",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 参数表格数据
      configList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        configName: "AI",
        configKey: "ai."
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        configName: [
          { required: true, message: "参数名称不能为空", trigger: "blur" },
          { min: 2, max: 100, message: '参数名称不能超过100个字符', trigger: 'blur' }
        ],
        configKey: [
          { required: true, message: "参数键名不能为空", trigger: "blur" },
          { min: 2, max: 100, message: '参数键名不能超过100个字符', trigger: 'blur' }
        ],
        configValue: [
          { required: true, message: "参数键值不能为空", trigger: "blur" },
          { min: 1, max: 500, message: '参数键值不能超过500个字符', trigger: 'blur' }
        ]
      },
      // 测试连接
      testResultVisible: false,
      testLoading: false,
      testSuccess: false,
      testResponse: "",
      testError: "",
      testModel: "",
      testResponseTime: 0,
      testProgress: 0,
      testStatus: "",
      testMessage: "正在连接AI服务..."
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询参数列表 */
    getList() {
      this.loading = true;
      // 只查询AI相关配置
      listConfig(this.queryParams).then(response => {
        this.configList = response.rows.filter(item => item.configKey.startsWith('ai.'));
        this.total = this.configList.length;
        this.loading = false;
      });
    },
    /** 取消按钮 */
    cancel() {
      this.open = false;
      this.reset();
    },
    /** 表单重置 */
    reset() {
      this.form = {
        configId: undefined,
        configName: undefined,
        configKey: undefined,
        configValue: undefined,
        remark: undefined
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.queryParams.configName = "AI";
      this.queryParams.configKey = "ai.";
      this.handleQuery();
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const configId = row.configId;
      getConfig(configId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改AI配置";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.configId != undefined) {
            updateConfig(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 刷新缓存按钮操作 */
    handleRefreshCache() {
      refreshCache().then(() => {
        this.$modal.msgSuccess("刷新成功");
      });
    },
    /** 测试连接按钮操作 */
    handleTestConnection() {
      this.testResultVisible = true;
      this.testLoading = true;
      this.testProgress = 0;
      this.testStatus = "";
      this.testMessage = "正在连接AI服务...";
      
      // 模拟进度条，但进度更慢，适应AI服务可能需要更长时间
      let progressIncrement = 5;
      const interval = setInterval(() => {
        // 前30秒，缓慢增长到75%
        if (this.testProgress < 75) {
          this.testProgress += progressIncrement;
          
          // 随着进度增加，逐渐减小增量速度
          if (this.testProgress > 50) {
            progressIncrement = 2;
          }
        }
        // 超过一定时间仍未响应，提示用户可能需要较长时间
        if (this.testProgress >= 75 && this.testProgress < 85) {
          this.testProgress += 1;
          this.testMessage = "AI服务响应可能需要较长时间，请耐心等待...";
        }
      }, 500);

      // 发送测试请求
      const testMessage = "请简单介绍一下你是什么AI助手?";
      sendMessageToAI({ message: testMessage })
        .then(response => {
          clearInterval(interval);
          this.testProgress = 100;
          this.testStatus = "success";
          this.testMessage = "连接成功!";
          
          // 详细打印响应结构
          console.log("完整响应对象:", response);
          console.log("响应类型:", typeof response);
          console.log("响应内容:", JSON.stringify(response, null, 2));
          
          if (response.data) {
            console.log("data字段类型:", typeof response.data);
            console.log("data字段内容:", JSON.stringify(response.data, null, 2));
          }
          
          setTimeout(() => {
            this.testLoading = false;
            this.testSuccess = true;
            
            try {
              // 直接从响应对象中提取所有可能包含数据的位置
              let extractedData = null;
              
              // 可能的数据位置1: response
              if (response && response.content) {
                extractedData = response;
                console.log("从response直接提取数据");
              }
              // 可能的数据位置2: response.data
              else if (response && response.data) {
                extractedData = response.data;
                console.log("从response.data提取数据");
              }
              // 可能的数据位置3: 在某些框架下可能是response.data.data
              else if (response && response.data && response.data.data) {
                extractedData = response.data.data;
                console.log("从response.data.data提取数据");
              }
              
              console.log("提取的数据:", extractedData);
              
              if (extractedData) {
                this.testResponse = extractedData.content || "无响应内容";
                this.testModel = extractedData.model || "未知模型";
                this.testResponseTime = extractedData.responseTime || 0;
                
                // 如果是模拟响应，显示提示
                if (extractedData.isSimulated) {
                  this.testResponse += "\n\n(注意：这是模拟响应，非真实AI服务返回内容)";
                }
              } else {
                this.testResponse = "无法从响应中提取数据";
                this.testModel = "未知";
                this.testResponseTime = 0;
              }
            } catch (err) {
              console.error("解析响应数据时出错:", err);
              this.testResponse = "解析响应数据时出错: " + err.message;
              this.testModel = "未知";
              this.testResponseTime = 0;
            }
          }, 500);
        })
        .catch(error => {
          clearInterval(interval);
          this.testProgress = 100;
          this.testStatus = "exception";
          this.testMessage = "连接失败!";
          
          // 调试输出错误信息
          console.error("AI请求错误:", error);
          
          setTimeout(() => {
            this.testLoading = false;
            this.testSuccess = false;
            this.testError = error.message || "未知错误";
          }, 500);
        });
    }
  }
};
</script> 