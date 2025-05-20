<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户昵称" prop="nickName">
        <el-input v-model="queryParams.nickName" placeholder="请输入用户昵称" clearable style="width: 240px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="queryParams.email" placeholder="请输入邮箱" clearable style="width: 240px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
        >新增</el-button>
      </el-col>
<!--      <el-col :span="1.5">-->
<!--        <el-button-->
<!--          type="success"-->
<!--          plain-->
<!--          icon="el-icon-edit"-->
<!--          size="mini"-->
<!--          :disabled="single"-->
<!--          @click="handleUpdate"-->
<!--        >修改</el-button>-->
<!--      </el-col>-->
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:book:export']"
        >导出
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
        >删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 用户数据表格 -->
    <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
<!--      <el-table-column label="用户ID" align="center" prop="_id" width="240"/>-->
      <el-table-column label="头像" align="center" width="100">
        <template slot-scope="scope">
          <img v-if="scope.row.info && scope.row.info.avatarUrl" :src="scope.row.info.avatarUrl" style="width:40px;height:40px;border-radius:50%;" alt="头像"/>
          <i v-else class="el-icon-user"></i>
        </template>
      </el-table-column>
      <el-table-column label="昵称" align="center" prop="info.nickName" :show-overflow-tooltip="true"/>
      <el-table-column label="邮箱" align="center" prop="email" :show-overflow-tooltip="true"/>
      <el-table-column label="余额" align="center" prop="parse" width="140"/>
      <el-table-column label="校区" align="center" width="150">
        <template slot-scope="scope">
          <span>{{ scope.row.campus ? scope.row.campus.name : '' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="100">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.useful" @change="handleStatusChange(scope.row)"></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" width="180">
        <template slot-scope="scope">
          <span>{{ formatDate(scope.row.stamp) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="120">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改用户对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="昵称" prop="nickName">
          <el-input v-model="form.nickName" placeholder="请输入用户昵称"/>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱"/>
        </el-form-item>
        <el-form-item label="密码" prop="pwd" v-if="!form._id">
          <el-input v-model="form.pwd" placeholder="请输入密码" show-password/>
        </el-form-item>
        <el-form-item label="校区" prop="campusName">
          <el-select v-model="form.campusName" placeholder="请选择校区" clearable>
            <el-option label="南望山校区" value="南望山校区"></el-option>
            <el-option label="未来城校区" value="未来城校区"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-switch v-model="form.useful"></el-switch>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: "UserManagement",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      userList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        nickName: "",
        email: ""
      },
      form: {
        _id: null,
        nickName: "",
        email: "",
        pwd: "",
        campusName: "",
        useful: true
      },
      rules: {
        email: [
          { required: true, message: "邮箱不能为空", trigger: "blur" },
          { type: "email", message: "邮箱格式不正确", trigger: "blur" }
        ],
        pwd: [
          { required: true, message: "密码不能为空", trigger: "blur" },
          { min: 5, max: 20, message: '密码长度必须介于5和20之间', trigger: 'blur' }
        ],
        campusName: [
          { required: true, message: "校区不能为空", trigger: "change" }
        ],
        nickName: [
          { required: true, message: "昵称不能为空", trigger: "blur" }
        ]
      }

    };
  },
  created() {
    this.getList();
  },
  methods: {
    async getList() {
      this.loading = true;
      try {
        const c1 = new this.cloud.Cloud({
          identityless: true,
          resourceAppid: 'wxe26635509bb0f6f3',
          resourceEnv: 'cloud1-0gjr8zke3634e2e7',
        });
        await c1.init();
        const res = await c1.callFunction({
          name: 'getAllUsers',
          data: {
            pageNum: this.queryParams.pageNum,
            pageSize: this.queryParams.pageSize,
            nickName: this.queryParams.nickName,
            email: this.queryParams.email
          }
        });
        console.log(res)
        if (res.result && res.result.success && res.result.data) {
          this.userList = res.result.data;
          this.total = res.result.total || this.userList.length;
        } else {
          this.userList = [];
          this.total = 0;
        }
        // console.log(this.userList)

      } catch (error) {
        console.error("获取用户数据失败:", error);
        this.userList = [];
        this.total = 0;
      } finally {
        this.loading = false;
      }
    },



    async handleExport() {
      // 假设 this.userList 是当前要导出的数据
      if (!this.userList || this.userList.length === 0) {
        this.$modal.msgWarning("没有数据可导出！");
        return;
      }

      // 将用户数据映射为可读的对象数组
      // 根据需要调整字段
      const data = this.userList.map(item => ({
        "用户ID": item._id,
        "昵称": item.info ? item.info.nickName : "",
        "邮箱": item.email,
        "余额": item.parse,
        "校区": item.campus ? item.campus.name : "",
        "状态": item.useful ? "启用" : "禁用",
        "创建时间": this.formatDate(item.stamp)
      }));

      // 使用 XLSX 将数据转化为 Excel
      const worksheet = XLSX.utils.json_to_sheet(data);
      const workbook = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(workbook, worksheet, "Users");

      // 将工作簿转换为二进制数据
      const wbout = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' });

      // 创建 Blob 对象
      const blob = new Blob([wbout], { type: 'application/octet-stream' });
      const url = URL.createObjectURL(blob);

      // 创建一个隐藏的 <a> 标签进行下载
      const link = document.createElement('a');
      link.href = url;
      link.download = `users_${new Date().getTime()}.xlsx`; // 自定义文件名
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    },


    // 状态切换
    async handleStatusChange(row) {
      // 假设useful字段true为启用，false为禁用
      // 直接更新该用户数据
      try {
        const c2 = new this.cloud.Cloud({
          identityless: true,
          resourceAppid: 'wxe26635509bb0f6f3',
          resourceEnv: 'cloud1-0gjr8zke3634e2e7',
        });
        await c2.init();
        const res = await c2.callFunction({
          name: 'updateUser',
          data: {
            _id: row._id,
            useful: row.useful
          }
        });
        if (!res.result || !res.result.success) {
          this.$modal.msgError("更新状态失败");
          row.useful = !row.useful; //回滚状态
        }
      } catch (error) {
        console.error("更新用户状态失败:", error);
        this.$modal.msgError("更新状态失败");
        row.useful = !row.useful; //回滚状态
      }
    },
    cancel() {
      this.open = false;
      this.resetForm();
    },
    resetForm() {
      this.form = {
        _id: null,
        nickName: "",
        email: "",
        pwd: "",
        campusName: "",
        useful: true
      };
      if (this.$refs.form) {
        this.$refs.form.resetFields();
      }
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.queryParams = {
        pageNum: 1,
        pageSize: 10,
        nickName: "",
        email: ""
      };
      this.getList();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item._id);
      this.single = selection.length != 1;
      this.multiple = selection.length === 0;
    },
    handleAdd() {
      this.resetForm();
      this.open = true;
      this.title = "添加用户";
    },
    handleUpdate(row) {
      const user = row || this.userList.find(item => item._id === this.ids[0]);
      this.form = {
        _id: user._id,
        nickName: user.info.nickName,
        email: user.email,
        pwd: "",
        campusName: user.campus ? user.campus.name : "",
        useful: user.useful
      };
      this.open = true;
      this.title = "修改用户";
    },
    async submitForm() {
      this.$refs["form"].validate(async (valid) => {
        if (valid) {
          try {
            const c3 = new this.cloud.Cloud({
              identityless: true,
              resourceAppid: 'wxe26635509bb0f6f3',
              resourceEnv: 'cloud1-0gjr8zke3634e2e7',
            });
            await c3.init();
            let res;
            if (this.form._id) {
              // 更新用户
              res = await c3.callFunction({
                name: 'updateUser',
                data: {
                  _id: this.form._id,
                  nickName: this.form.nickName,
                  email: this.form.email,
                  campusName: this.form.campusName,
                  useful: this.form.useful,
                  pwd: this.form.pwd
                }
              });
            } else {
              // 新增用户
              res = await c3.callFunction({
                name: 'addUser',
                data: {
                  nickName: this.form.nickName,
                  email: this.form.email,
                  pwd: this.form.pwd,
                  campusName: this.form.campusName,
                  useful: this.form.useful,
                  }
              });
            }
            console.log(res)
            if (res.result && res.result.success) {
              this.$modal.msgSuccess(this.form._id ? "修改成功" : "新增成功");
              this.open = false;
              this.getList();
            } else {
              this.$modal.msgError(this.form._id ? "修改失败" : "新增失败");
            }
          } catch (error) {
            console.error(this.form._id ? "修改用户失败:" : "新增用户失败:", error);
            this.$modal.msgError(this.form._id ? "修改失败" : "新增失败");
          }
        }
      });
    },
    async handleDelete(row) {
      let userIds;
      if (row && row._id) {
        userIds = [row._id];
      } else {
        userIds = this.ids;
      }

      // console.log(row)
      // console.log(row._id)
      // console.log('this.ids:', this.ids); // 输出正确的值
      // console.log('userIds:', userIds);   // 应该也正确
      if (userIds.length < 1) {
        return
      }

      // console.log(this.ids)

      this.$modal.confirm('是否确认删除这些用户数据？').then(async () => {
        try {
          const c4 = new this.cloud.Cloud({
            identityless: true,
            resourceAppid: 'wxe26635509bb0f6f3',
            resourceEnv: 'cloud1-0gjr8zke3634e2e7',
          });
          await c4.init();
          const res = await c4.callFunction({
            name: 'deleteUser',
            data: {
              _id: userIds.length === 1 ? userIds[0] : userIds
            }
          });

          // console.log(res)
          // console.log(userIds)

          if (res.result && res.result.success) {
            this.$modal.msgSuccess("删除成功");
            this.getList();
          } else {
            this.$modal.msgError("删除失败");
          }
        } catch (error) {
          console.error("删除用户失败:", error);
          this.$modal.msgError("删除失败");
        }
      }).catch(() => {});
    },
    formatDate(timestamp) {
      if (!timestamp) return "";
      const date = new Date(timestamp);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, "0");
      const day = String(date.getDate()).padStart(2, "0");
      const hours = String(date.getHours()).padStart(2, "0");
      const minutes = String(date.getMinutes()).padStart(2, "0");
      const seconds = String(date.getSeconds()).padStart(2, "0");
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    }
  }
};
</script>
