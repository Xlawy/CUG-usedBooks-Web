<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="交易类型">
        <el-select v-model="queryParams.type" clearable placeholder="选择交易类型" style="width: 200px;">
          <el-option :value="1" label="钱包充值"></el-option>
          <el-option :value="2" label="余额提现"></el-option>
          <el-option :value="3" label="购买书籍"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="关键字">
        <el-input v-model="queryParams.name" clearable placeholder="交易名称" style="width: 200px;"></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <!--      <el-col :span="1.5">-->
      <!--        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd">新增</el-button>-->
      <!--      </el-col>-->
      <!--      <el-col :span="1.5">-->
      <!--        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate">修改</el-button>-->
      <!--      </el-col>-->
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button icon="el-icon-download" plain size="mini" type="warning" @click="handleExport">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="historyList" @selection-change="handleSelectionChange">
      <el-table-column align="center" type="selection" width="50"></el-table-column>
      <el-table-column label="ID" prop="_id" width="320" align="center"></el-table-column>
      <el-table-column label="交易名称" prop="name" width="200" align="center"></el-table-column>
      <el-table-column align="center" label="金额" width="140" >
        <template slot-scope="scope">
          <!-- 根据 type 动态显示 +num 或 -num -->
          <span :style="{ color: scope.row.type === 1 ? 'green' : 'red' }">
      {{ scope.row.type === 1 ? '+' : '-' }}{{ scope.row.num }}
    </span>
        </template>
      </el-table-column>
      <el-table-column label="用户ID" prop="oid" width="310" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.oid || "无" }}</span>
        </template>
      </el-table-column>
      <el-table-column label="交易时间" prop="stamp" width="240" align="center">
        <template slot-scope="scope">
          <span>{{ formatDate(scope.row.stamp) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      :limit.sync="queryParams.pageSize"
      :page.sync="queryParams.pageNum"
      :total="total"
      @pagination="getList"
    />

    <!-- 添加/修改对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="交易名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入交易名称"></el-input>
        </el-form-item>
        <el-form-item label="金额" prop="num">
          <el-input-number v-model="form.num" :min="0" placeholder="请输入金额"></el-input-number>
        </el-form-item>
        <el-form-item label="交易类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择交易类型">
            <el-option :value="1" label="+"></el-option>
            <el-option :value="2" label="-"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="用户ID" prop="oid">
          <el-input v-model="form.oid" placeholder="请输入用户ID"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: "HistoryRecords",
  data() {
    return {
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        type: undefined,
        name: "",
      },
      historyList: [],
      multiple: true,
      showSearch: true,
      total: 0,
      loading: false,
      open: false,
      title: "",
      form: {
        _id: null,
        name: "",
        num: 0,
        type: null,
        oid: "",
        stamp: null,
      },
      rules: {
        name: [{required: true, message: "请输入交易名称", trigger: "blur"}],
        num: [{required: true, message: "请输入金额", trigger: "blur"}],
        type: [{required: true, message: "请选择交易类型", trigger: "change"}],
      },
    };
  },
  mounted() {
    this.getList();
  },
  methods: {
    async getList() {
      this.loading = true;
      try {
        // 调用微信小程序云函数获取数据
        const c1 = new this.cloud.Cloud({
          identityless: true,
          resourceAppid: 'wxe26635509bb0f6f3',
          resourceEnv: 'zjh-3g1jn04nc036f36e',
        });
        await c1.init();
        const res = await c1.callFunction({
          name: "getHistoryRecords",
          data: {
            pageNum: this.queryParams.pageNum,
            pageSize: this.queryParams.pageSize,
            type: this.queryParams.type,
            name: this.queryParams.name,
          },
        });
        if (res.result && res.result.success) {
          this.historyList = res.result.data;
          // console.log(this.historyList)

          this.total = res.result.total || 0;
        } else {
          this.historyList = [];
          this.total = 0;
        }
      } catch (error) {
        console.error("获取数据失败:", error);
        this.$message.error("获取数据失败");
      } finally {
        this.loading = false;
      }
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item._id);
      this.single = selection.length != 1;
      this.multiple = selection.length === 0;
    },
    handleExport() {
      if (!this.historyList || this.historyList.length === 0) {
        this.$modal.msgWarning("无数据可导出");
        return;
      }

      const data = this.historyList.map(item => ({
        "ID": item._id,
        "交易名称": item.name,
        "金额": item.type === 1 ? `+${item.num}` : `-${item.num}`, // 根据 type 添加符号
        "用户ID": item._openid,
        "交易时间": this.formatDate(item.stamp), // 转换时间格式
      }));

      const worksheet = XLSX.utils.json_to_sheet(data);
      const workbook = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(workbook, worksheet, "订单列表");

      const wbout = XLSX.write(workbook, {bookType: 'xlsx', type: 'array'});
      const blob = new Blob([wbout], {type: 'application/octet-stream'});
      const url = URL.createObjectURL(blob);

      const link = document.createElement('a');
      link.href = url;
      link.download = `orders_${new Date().getTime()}.xlsx`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.queryParams = {pageNum: 1, pageSize: 10, type: undefined, name: ""};
      this.getList();
    },
    handleUpdate(row) {
      this.form = {...row};
      this.title = "修改交易记录";
      this.open = true;
    },
    async handleDelete(row) {
      let ids;
      if (row && row._id) {
        ids = [row._id];
      } else {
        ids = this.ids;
      }
      if (ids.length < 1) return;
      this.$modal.confirm("是否确认删除选中的数据项？").then(async () => {
        try {
          const c1 = new this.cloud.Cloud({
            identityless: true,
            resourceAppid: 'wxe26635509bb0f6f3',
            resourceEnv: 'zjh-3g1jn04nc036f36e',
          });
          await c1.init();
          const res = await c1.callFunction({
            name: "deleteHistoryRecord",
            data: {
              _id: ids.length === 1 ? ids[0] : ids
            }
          });
          console.log(res)
          if (res.result && res.result.success) {
            this.$message.success("删除成功");
            this.getList();
          } else {
            this.$message.error("删除失败");
          }
        } catch (error) {
          console.error("删除失败:", error);
          this.$message.error("删除失败");
        }
      }).catch(() => {
      })
    },
    async submitForm() {
      this.$refs.form.validate(async (valid) => {
        if (valid) {
          try {
            const c1 = new this.cloud.Cloud({
              identityless: true,
              resourceAppid: 'wxe26635509bb0f6f3',
              resourceEnv: 'zjh-3g1jn04nc036f36e',
            });
            await c1.init();
            const res = await c1.callFunction({
              name: "saveHistoryRecord",
              data: this.form,
            });
            if (res.result && res.result.success) {
              this.$message.success(this.form._id ? "修改成功" : "新增成功");
              this.open = false;
              this.getList();
            } else {
              this.$message.error(this.form._id ? "修改失败" : "新增失败");
            }
          } catch (error) {
            console.error(error);
            this.$message.error(this.form._id ? "修改失败" : "新增失败");
          }
        }
      });
    },
    formatDate(timestamp) {
      if (!timestamp) return "";
      const date = new Date(timestamp);
      return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, "0")}-${date
        .getDate()
        .toString()
        .padStart(2, "0")} ${date.getHours().toString().padStart(2, "0")}:${date
        .getMinutes()
        .toString()
        .padStart(2, "0")}:${date.getSeconds().toString().padStart(2, "0")}`;
    },
  },
};
</script>
