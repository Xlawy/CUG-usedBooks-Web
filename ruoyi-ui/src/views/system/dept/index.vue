<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="书名">
        <el-input v-model="queryParams.title" placeholder="书名" clearable style="width:240px" @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width:240px">
          <el-option :value="0" label="在售"></el-option>
          <el-option :value="1" label="买家已付款，但卖家未发货"></el-option>
          <el-option :value="2" label="买家确认收货，交易完成"></el-option>
          <el-option :value="3" label="交易作废，退还买家钱款"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="卖家">
        <el-input v-model="queryParams.nickName" placeholder="卖家" clearable style="width:240px" @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="orderList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center"/>

      <!-- 新增封面列 -->
      <el-table-column label="封面" width="100" align="center">
        <template slot-scope="scope">
          <img v-if="scope.row.bookinfo && scope.row.bookinfo.pic" :src="scope.row.bookinfo.pic" style="width:60px;height:auto;"/>
        </template>
      </el-table-column>

      <el-table-column label="书名" prop="bookinfo.title" width="100" :show-overflow-tooltip="true" align="center"/>
      <el-table-column label="价格" prop="price" width="120" align="center" />
      <el-table-column label="卖家" prop="sellerNickName" width="100" :show-overflow-tooltip="true" align="center"/>
      <el-table-column label="状态" width="260" align="center">
        <template slot-scope="scope">
          <span v-if="scope.row.status === 0">在售</span>
          <span v-else-if="scope.row.status === 1">买家已付款，但卖家未发货</span>
          <span v-else-if="scope.row.status === 2">买家确认收货，交易完成</span>
          <span v-else-if="scope.row.status === 3">交易作废，退还买家钱款</span>
        </template>
      </el-table-column>
      <el-table-column label="自提/配送" width="150" align="center">
        <template slot-scope="scope">
          <span v-if="scope.row.deliveryid === 1">卖家配送</span>
          <span v-else-if="scope.row.deliveryid === 0">买家自提</span>
        </template>
      </el-table-column>
      <el-table-column label="自提点" prop="ztplace" width="120" align="center"/>
      <el-table-column label="配送地址" prop="psplace" width="120" align="center"/>
      <el-table-column label="创建时间" prop="creat" width="180" align="center">
        <template slot-scope="scope">
          <span>{{ formatDate(scope.row.creat) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>

    <!-- 添加或修改对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="书名" prop="title">
          <el-input v-model="form.title" placeholder="请输入书名"/>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0"/>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态">
            <el-option :value="0" label="在售"></el-option>
            <el-option :value="1" label="买家已付款，但卖家未发货"></el-option>
            <el-option :value="2" label="买家确认收货，交易完成"></el-option>
            <el-option :value="3" label="交易作废，退还买家钱款"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="卖家" prop="seller">
          <el-input v-model="form.seller" placeholder="请输入卖家ID"/>
        </el-form-item>

        <!-- 新增封面URL编辑项和预览 -->
        <el-form-item label="封面">
          <el-input v-model="form.pic" placeholder="图片URL"/>
          <div style="margin-top:5px;" v-if="form.pic">
            <img :src="form.pic" style="width:100px;height:auto;"/>
          </div>
        </el-form-item>

        <el-form-item label="自提点">
          <el-input v-model="form.ztplace"/>
        </el-form-item>
        <el-form-item label="配送地址">
          <el-input v-model="form.psplace"/>
        </el-form-item>
        <el-form-item label="配送方式ID">
          <el-input-number v-model="form.deliveryid" :min="0"/>
        </el-form-item>
        <!-- 根据需要可增添对 bookinfo.author/edition 等字段的编辑项 -->
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确定</el-button>
        <el-button @click="cancel">取消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: "OrderManagement",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      orderList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: "",
        status: undefined,
        seller: "",
        nickName: ""
      },
      form: {
        _id: null,
        title: "",
        price: 0,
        status: 0,
        seller: "",
        ztplace: "",
        psplace: "",
        deliveryid: 0,
        pic: "" // 新增封面URL字段
      },
      rules: {
        title: [{ required: true, message: "书名不能为空", trigger: "blur" }],
        price: [{ required: true, message: "价格不能为空", trigger: "blur" }],
        seller: [{ required: true, message: "卖家不能为空", trigger: "blur" }]
      }
    };
  },
  created() {
    this.getList();
    // this.getNickName();
  },
  methods: {
    async getList() {
      this.loading = true;
      try {
        const c1 = new this.cloud.Cloud({
          identityless: true,
          resourceAppid: 'wxe26635509bb0f6f3',
          resourceEnv: 'zjh-3g1jn04nc036f36e',
        });
        await c1.init();
        const res = await c1.callFunction({
          name: 'getAllOrders',
          data: {
            pageNum: this.queryParams.pageNum,
            pageSize: this.queryParams.pageSize,
            title: this.queryParams.title,
            status: this.queryParams.status,
            seller: this.queryParams.seller,
          }
        });
        if (res.result && res.result.success && res.result.data) {
          this.orderList = res.result.data;
          console.log(this.orderList)
          this.total = res.result.total || this.orderList.length;
        } else {
          this.orderList = [];
          this.total = 0;
        }
      } catch (error) {
        console.error("获取订单数据失败:", error);
        this.orderList = [];
        this.total = 0;
      } finally {
        this.loading = false;
      }
    },
    // async getNickName() {
    //   this.loading = true;
    //   try {
    //     const c1 = new this.cloud.Cloud({
    //       identityless: true,
    //       resourceAppid: 'wxe26635509bb0f6f3',
    //       resourceEnv: 'zjh-3g1jn04nc036f36e',
    //     });
    //     await c1.init();
    //     const res = await c1.callFunction({
    //       name: 'getAllUsers',
    //       data: {
    //         pageNum: this.queryParams.pageNum,
    //         pageSize: this.queryParams.pageSize,
    //         open_id: this.queryParams.seller,
    //       }
    //     });
    //     if (res.result && res.result.success && res.result.data) {
    //       this.total = res.result.total || 0;
    //     } else {
    //       this.userList = [];
    //       this.total = 0;
    //     }
    //   } catch (error) {
    //     console.error("获取用户数据失败:", error);
    //     this.userList = [];
    //     this.total = 0;
    //   } finally {
    //     this.loading = false;
    //   }
    // },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.queryParams = {
        pageNum: 1,
        pageSize: 10,
        title: "",
        status: undefined,
        seller: ""
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
      this.title = "添加订单";
    },
    handleUpdate(row) {
      const data = row || this.orderList.find(item => item._id === this.ids[0]);
      this.form = {
        _id: data._id,
        title: data.bookinfo.title,
        price: data.price,
        status: data.status,
        seller: data.seller,
        ztplace: data.ztplace || "",
        psplace: data.psplace || "",
        deliveryid: data.deliveryid || 0,
        pic: data.bookinfo.pic || ""
      };
      this.open = true;
      this.title = "修改订单";
    },
    cancel() {
      this.open = false;
      this.resetForm();
    },
    resetForm() {
      this.form = {
        _id: null,
        title: "",
        price: 0,
        status: 0,
        seller: "",
        ztplace: "",
        psplace: "",
        deliveryid: 0,
        pic: ""
      };
      if (this.$refs.form) {
        this.$refs.form.resetFields();
      }
    },
    async submitForm() {
      this.$refs.form.validate(async (valid) => {
        if (valid) {
          try {
            const c2 = new this.cloud.Cloud({
              identityless: true,
              resourceAppid: 'wxe26635509bb0f6f3',
              resourceEnv: 'zjh-3g1jn04nc036f36e',
            });
            await c2.init();
            let res;
            if (this.form._id) {
              // update
              res = await c2.callFunction({
                name: 'updateOrder',
                data: {
                  _id: this.form._id,
                  title: this.form.title,
                  price: this.form.price,
                  status: this.form.status,
                  seller: this.form.seller,
                  ztplace: this.form.ztplace,
                  psplace: this.form.psplace,
                  deliveryid: this.form.deliveryid,
                  pic: this.form.pic  // 更新封面URL
                }
              });
            } else {
              // add
              res = await c2.callFunction({
                name: 'addOrder',
                data: {
                  title: this.form.title,
                  price: this.form.price,
                  status: this.form.status,
                  seller: this.form.seller,
                  ztplace: this.form.ztplace,
                  psplace: this.form.psplace,
                  deliveryid: this.form.deliveryid,
                  pic: this.form.pic // 新增时传递封面URL
                }
              });
            }

            if (res.result && res.result.success) {
              this.$modal.msgSuccess(this.form._id ? "修改成功" : "新增成功");
              this.open = false;
              this.getList();
            } else {
              this.$modal.msgError(this.form._id ? "修改失败" : "新增失败");
            }
          } catch (error) {
            console.error(this.form._id ? "修改失败:" : "新增失败:", error);
            this.$modal.msgError(this.form._id ? "修改失败" : "新增失败");
          }
        }
      });
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
          const c3 = new this.cloud.Cloud({
            identityless: true,
            resourceAppid: 'wxe26635509bb0f6f3',
            resourceEnv: 'zjh-3g1jn04nc036f36e',
          });
          await c3.init();
          const res = await c3.callFunction({
            name: 'deleteOrder',
            data: {
              _id: ids.length === 1 ? ids[0] : ids
            }
          });
          if (res.result && res.result.success) {
            this.$modal.msgSuccess("删除成功");
            this.getList();
          } else {
            this.$modal.msgError("删除失败");
          }
        } catch (error) {
          console.error("删除失败:", error);
          this.$modal.msgError("删除失败");
        }
      }).catch(() => {});
    },
    handleStatusChange(row) {
      this.submitUpdateStatus(row);
    },
    async submitUpdateStatus(row) {
      try {
        const c4 = new this.cloud.Cloud({
          identityless: true,
          resourceAppid: 'wxe26635509bb0f6f3',
          resourceEnv: 'zjh-3g1jn04nc036f36e',
        });
        await c4.init();
        const res = await c4.callFunction({
          name: 'updateOrder',
          data: {
            _id: row._id,
            status: row.status
          }
        });
        if (!(res.result && res.result.success)) {
          this.$modal.msgError("状态更新失败");
          row.status = row.status === 0 ? 1 : 0; // rollback
        }
      } catch (error) {
        console.error("更新状态失败:", error);
        this.$modal.msgError("更新状态失败");
        row.status = row.status === 0 ? 1 : 0; // rollback
      }
    },
    handleExport() {
      if (!this.orderList || this.orderList.length === 0) {
        this.$modal.msgWarning("无数据可导出");
        return;
      }

      const data = this.orderList.map(item => ({
        "书名": item.bookinfo.title,
        "价格": item.price,
        "卖家": item.seller,
        "状态": item.status,
        "自提点": item.ztplace,
        "配送地址": item.psplace,
        "创建时间": this.formatDate(item.creat)
      }));

      const worksheet = XLSX.utils.json_to_sheet(data);
      const workbook = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(workbook, worksheet, "订单列表");

      const wbout = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' });
      const blob = new Blob([wbout], { type: 'application/octet-stream' });
      const url = URL.createObjectURL(blob);

      const link = document.createElement('a');
      link.href = url;
      link.download = `orders_${new Date().getTime()}.xlsx`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
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
