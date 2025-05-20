<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch">
      <el-form-item label="书名">
        <el-input v-model="queryParams.title" placeholder="书名" clearable style="width:240px" @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="作者">
        <el-input v-model="queryParams.author" placeholder="作者" clearable style="width:240px" @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width:240px" >
          <el-option :value="0" label="在售"></el-option>
          <el-option :value="1" label="买家已付款，但卖家未发货"></el-option>
          <el-option :value="2" label="买家确认收货，交易完成"></el-option>
        </el-select>
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
    <el-table v-loading="loading" :data="bookList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center"/>

      <!-- 新增封面列 -->
      <el-table-column label="封面" width="100" align="center">
        <template slot-scope="scope">
          <img v-if="scope.row.bookinfo && scope.row.bookinfo.pic" :src="scope.row.bookinfo.pic" style="width:60px;height:auto;"/>
        </template>
      </el-table-column>

      <el-table-column label="书名" prop="bookinfo.title" width="200" :show-overflow-tooltip="true" align="center"/>
      <el-table-column label="作者" prop="bookinfo.author" width="200" :show-overflow-tooltip="true" align="center"/>
      <el-table-column label="价格" prop="price" width="150" align="center"/>
      <el-table-column label="状态" width="250" align="center">
        <template slot-scope="scope">
          <span v-if="scope.row.status === 0">在售</span>
          <span v-else-if="scope.row.status === 1">买家已付款，但卖家未发货</span>
          <span v-else-if="scope.row.status === 2">买家确认收货，交易完成</span>
        </template>
      </el-table-column>
      <el-table-column label="分类" prop="collegeid" width="125" align="center">
        <template slot-scope="scope">
          <span>{{ getCollegeName(scope.row.collegeid) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="creat" width="225" align="center">
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
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="书名" prop="title">
          <el-input v-model="form.title" placeholder="请输入书名"/>
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="form.author" placeholder="请输入作者"/>
        </el-form-item>

        <el-form-item label="封面">
          <el-input v-model="form.pic" placeholder="图片URL"/>
          <div style="margin-top:5px;" v-if="form.pic">
            <img :src="form.pic" style="width:100px;height:auto;"/>
          </div>
        </el-form-item>

        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0"/>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态">
            <el-option :value="0" label="在售"></el-option>
            <el-option :value="1" label="买家已付款，但卖家未发货"></el-option>
            <el-option :value="2" label="买家确认收货，交易完成"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="分类ID">
          <el-select v-model="form.collegeid" placeholder="请选择分类">
            <el-option
              v-for="item in college"
              :key="item.id"
              :label="item.name"
              :value="item.id">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.notes" type="textarea" placeholder="请输入备注"/>
        </el-form-item>
        <el-form-item label="配送方式">
          <el-select v-model="form.deliveryid" placeholder="请选择配送方式">
            <el-option :value="0" label="买家自提"></el-option>
            <el-option :value="1" label="卖家帮送"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="form.place"/>
        </el-form-item>
        <el-form-item label="关键词key">
          <el-input v-model="form.key"/>
        </el-form-item>
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
  name: "PublishedBookManagement",
  data() {
    return {
      college: [
        { name: '通用', id: -1 },
        { name: '计算机', id: 0 },
        { name: '地信', id: 1 },
        { name: '环境', id: 2 },
        { name: '经管', id: 3 },
        { name: '材化', id: 4 },
        { name: '英语', id: 5 },
        { name: '地质', id: 6 },
        { name: '珠宝', id: 7 },
        { name: '自动化', id: 8 },
        { name: '艺媒', id: 9 },
        { name: '体育', id: 10 },
        { name: '工程', id: 11 },
        { name: '机电', id: 12 },
        { name: '公管', id: 13 },
        { name: '马克思', id: 14 },
        { name: '海洋', id: 15 },
        { name: '新能源', id: 16 },
        { name: '李四光学院', id: 17 },
        { name: '其他', id: 18 },
      ],
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      bookList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: "",
        author: "",
        status: undefined
      },
      form: {
        _id: null,
        title: "",
        author: "",
        pic: "",
        price: 0,
        status: 0,
        notes: "",
        kindid: "0",
        deliveryid: 0,
        collegeid: "",
        place: "",
        key: "",
        dura: null
      },
      rules: {
        title: [{ required: true, message: "书名不能为空", trigger: "blur" }],
        author: [{ required: true, message: "作者不能为空", trigger: "blur" }],
        price: [{ required: true, message: "价格不能为空", trigger: "blur" }]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getCollegeName(collegeid) {
      const college = this.college.find(item => item.id === Number(collegeid));
      return college ? college.name : '未知';
    },
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
          name: 'getAllPublishedBooks',
          data: {
            pageNum: this.queryParams.pageNum,
            pageSize: this.queryParams.pageSize,
            title: this.queryParams.title,
            author: this.queryParams.author,
            status: this.queryParams.status
          }
        });
        if (res.result && res.result.success && res.result.data) {
          this.bookList = res.result.data;
          this.total = res.result.total || this.bookList.length;
        } else {
          this.bookList = [];
          this.total = 0;
        }
      } catch (error) {
        console.error("获取数据失败:", error);
        this.bookList = [];
        this.total = 0;
      } finally {
        this.loading = false;
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
        title: "",
        author: "",
        status: undefined
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
      this.title = "添加书籍发布";
    },
    handleUpdate(row) {
      const data = row || this.bookList.find(item => item._id === this.ids[0]);
      this.form = {
        _id: data._id,
        title: data.bookinfo.title,
        author: data.bookinfo.author,
        pic: data.bookinfo.pic,
        price: data.price,
        status: data.status,
        notes: data.notes || "",
        kindid: data.kindid || "0",
        deliveryid: data.deliveryid || 0,
        collegeid: data.collegeid || "",
        place: data.place || "",
        key: data.key || "",
        dura: data.dura ? new Date(data.dura) : null
      };
      this.open = true;
      this.title = "修改书籍发布";
    },
    cancel() {
      this.open = false;
      this.resetForm();
    },
    resetForm() {
      this.form = {
        _id: null,
        title: "",
        author: "",
        pic: "",
        price: 0,
        status: 0,
        notes: "",
        kindid: "0",
        deliveryid: 0,
        collegeid: "",
        place: "",
        key: "",
        dura: null
      };
      if (this.$refs.form) {
        this.$refs.form.resetFields();
      }
    },
    async submitForm() {
      this.$refs.form.validate(async valid => {
        if (valid) {
          try {
            const c2 = new this.cloud.Cloud({
              identityless: true,
              resourceAppid: 'wxe26635509bb0f6f3',
              resourceEnv: 'cloud1-0gjr8zke3634e2e7',
            });
            await c2.init();
            let res;
            if (this.form._id) {
              // update
              res = await c2.callFunction({
                name: 'updatePublishedBook',
                data: {
                  _id: this.form._id,
                  title: this.form.title,
                  author: this.form.author,
                  pic: this.form.pic,
                  price: this.form.price,
                  status: this.form.status,
                  notes: this.form.notes,
                  kindid: this.form.kindid,
                  deliveryid: this.form.deliveryid,
                  collegeid: this.form.collegeid,
                  place: this.form.place,
                  key: this.form.key,
                  dura: this.form.dura ? this.form.dura.getTime() : null
                }
              });
            } else {
              // add
              res = await c2.callFunction({
                name: 'addPublishedBook',
                data: {
                  title: this.form.title,
                  author: this.form.author,
                  pic: this.form.pic,
                  price: this.form.price,
                  status: this.form.status,
                  notes: this.form.notes,
                  kindid: this.form.kindid,
                  deliveryid: this.form.deliveryid,
                  collegeid: this.form.collegeid,
                  place: this.form.place,
                  key: this.form.key,
                  dura: this.form.dura ? this.form.dura.getTime() : null
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
            resourceEnv: 'cloud1-0gjr8zke3634e2e7',
          });
          await c3.init();
          const res = await c3.callFunction({
            name: 'deletePublishedBook',
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
          resourceEnv: 'cloud1-0gjr8zke3634e2e7',
        });
        await c4.init();
        const res = await c4.callFunction({
          name: 'updatePublishedBook',
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
      if (!this.bookList || this.bookList.length === 0) {
        this.$modal.msgWarning("无数据可导出");
        return;
      }

      const data = this.bookList.map(item => ({
        "书名": item.bookinfo.title,
        "作者": item.bookinfo.author,
        "价格": item.price,
        "状态": item.status === 0 ? "正常" : "下架",
        "分类ID": item.collegeid,
        "创建时间": this.formatDate(item.creat)
      }));

      const worksheet = XLSX.utils.json_to_sheet(data);
      const workbook = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(workbook, worksheet, "发布书籍");

      const wbout = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' });
      const blob = new Blob([wbout], { type: 'application/octet-stream' });
      const url = URL.createObjectURL(blob);

      const link = document.createElement('a');
      link.href = url;
      link.download = `published_books_${new Date().getTime()}.xlsx`;
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
