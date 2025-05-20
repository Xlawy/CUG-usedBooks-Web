

<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch">
      <el-form-item label="书名" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入书名"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="作者" prop="author">
        <el-input
          v-model="queryParams.author"
          placeholder="请输入作者"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="ISBN" prop="isbn">
        <el-input
          v-model="queryParams.isbn"
          placeholder="请输入ISBN"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <!-- 可以根据需要添加更多搜索条件，如出版社、出版日期等 -->
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
          v-hasPermi="['system:book:add']"
        >新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:book:edit']"
        >修改
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
          v-hasPermi="['system:book:remove']"
        >删除
        </el-button>
      </el-col>
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
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 书籍数据表格 -->
    <el-table v-loading="loading" :data="bookList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
<!--      <el-table-column label="书籍编号" prop="_id" width="180"/>-->
      <el-table-column label="封面" width="180">
        <template slot-scope="scope">
          <img
            v-if="scope.row.pic"
            :src="scope.row.pic"
            alt="书籍封面"
            style="width:60px;height:auto;"
          />
          <img
            v-else
            :src="defaultCoverImg"
            alt="暂无封面"
            style="width:60px;height:auto;"
          />
        </template>
      </el-table-column>
      <el-table-column label="书名" prop="title" :show-overflow-tooltip="true" width="200"/>
      <el-table-column label="作者" prop="author" :show-overflow-tooltip="true" width="150"/>
      <el-table-column label="ISBN" prop="isbn" width="150"/>
      <el-table-column label="出版社" prop="publisher" width="180"/>
      <el-table-column label="定价" prop="price" width="100"/>
      <el-table-column label="出版日期" prop="pubdate" width="150">
        <template slot-scope="scope">
          <span>{{ formatDate(scope.row.pubdate) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope" v-if="scope.row._id">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:book:edit']"
          >修改
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:book:remove']"
          >删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页组件 -->
    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改书籍的对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="书名" prop="title">
          <el-input v-model="form.title" placeholder="请输入书名"/>
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="form.author" placeholder="请输入作者"/>
        </el-form-item>
        <el-form-item label="ISBN" prop="isbn">
          <el-input v-model="form.isbn" placeholder="请输入ISBN"/>
        </el-form-item>
        <el-form-item label="出版社" prop="publisher">
          <el-input v-model="form.publisher" placeholder="请输入出版社"/>
        </el-form-item>
        <el-form-item label="定价" prop="price">
          <el-input-number v-model="form.price" controls-position="right" :min="0"/>
        </el-form-item>
        <el-form-item label="出版日期" prop="pubdate">
          <el-date-picker
            v-model="form.pubdate"
            type="date"
            placeholder="选择日期"
            style="width: 100%"
            value-format="yyyy-MM-dd"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="封面图片">
          <el-upload
            class="avatar-uploader"
            :show-file-list="false"
            :before-upload="beforeCoverUpload"
          >
            <img alt="" v-if="form.picUrl" :src="form.picUrl" class="avatar" />
            <i v-else class="el-icon-plus avatar-uploader-icon"></i>
          </el-upload>
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" placeholder="请输入摘要"></el-input>
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
  name: "BookManagement",
  data() {
    return {
      // 默认封面
      defaultCoverImg: require('@/assets/images/default-cover.png'),
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 书籍表格数据
      bookList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: "",
        author: "",
        isbn: ""
      },
      // 表单参数
      form: {
        _id: null,
        title: "",
        author: "",
        isbn: "",
        publisher: "",
        price: 0,
        pubdate: "",
        pic: "",
        picUrl:"",
        summary: ""
      },
      // 表单校验
      rules: {
        title: [
          { required: true, message: "书名不能为空", trigger: "blur" }
        ],
        author: [
          { required: true, message: "作者不能为空", trigger: "blur" }
        ],
        isbn: [
          { required: true, message: "ISBN不能为空", trigger: "blur" }
        ],
        publisher: [
          { required: true, message: "出版社不能为空", trigger: "blur" }
        ],
        price: [
          { required: true, message: "定价不能为空", trigger: "blur" }
        ],
        pubdate: [
          { required: true, message: "出版日期不能为空", trigger: "change" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {

    async beforeCoverUpload(file) {
      // 用户选择图片后立即上传到云存储
      try {
        const base64 = await this.fileToBase64(file);

        // 调用uploadBookCover云函数上传图片
        const c5 = new this.cloud.Cloud({
          identityless: true,
          resourceAppid: 'wxe26635509bb0f6f3',
          resourceEnv: 'zjh-3g1jn04nc036f36e',
        });
        await c5.init();

        const uploadRes = await c5.callFunction({
          name: 'uploadBookCover',
          data: {
            fileContent: base64,
            filename: file.name
          }
        });

        console.log(uploadRes)
        if (uploadRes.result && uploadRes.result.success) {
          const fileID = uploadRes.result.fileID;
          // 通过getTempFileURL获取临时URL预览
          const tempRes = await c5.callFunction({
            name: 'getTempFileURL',
            data: { fileList: [fileID] }
          });
          console.log(tempRes)
          if (tempRes.result && tempRes.result.success && tempRes.result.fileList && tempRes.result.fileList[0].tempFileURL) {
            this.form.pic = fileID;
            this.form.picUrl = tempRes.result.fileList[0].tempFileURL;
            this.$modal.msgSuccess("封面上传成功");
          } else {
            this.$modal.msgError("获取图片URL失败");
          }
        } else {
          this.$modal.msgError("上传图片失败");
        }
      } catch (error) {
        console.error("图片上传失败:", error);
        this.$modal.msgError("图片上传失败");
      }

      // 阻止Element默认上传行为
      return false;
    },

    fileToBase64(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = () => {
          const base64String = reader.result.split(',')[1];
          resolve(base64String);
        };
        reader.onerror = error => reject(error);
      });
    },




    /** 获取书籍列表 */
    async getList() {
      this.loading = true;
      try {
        // 调用微信小程序云函数获取书籍数据
        const c1 = new this.cloud.Cloud({
          identityless: true,
          resourceAppid: 'wxe26635509bb0f6f3',
          resourceEnv: 'zjh-3g1jn04nc036f36e',
        });
        await c1.init();

        const res = await c1.callFunction({
          name: 'getAllBooks',
          data: {
            pageNum: this.queryParams.pageNum,
            pageSize: this.queryParams.pageSize,
            title: this.queryParams.title || '',
            author: this.queryParams.author || '',
            isbn: this.queryParams.isbn || ''
          }
        });

        // 根据云函数的返回结果处理数据
        if (res.result && res.result.success && res.result.data) {
          const books = res.result.data;
          // 收集cloud://开头的pic
          const cloudFileIDs = books
            .filter(book => book.pic && book.pic.startsWith('cloud://'))
            .map(book => book.pic);

          if (cloudFileIDs.length > 0) {
            // 调用云函数将cloud://开头的fileID转换为临时URL
            const tempRes = await c1.callFunction({
              name: 'getTempFileURL',
              data: { fileList: cloudFileIDs }
            });

            if (tempRes.result && tempRes.result.success && tempRes.result.fileList) {
              // 将结果映射成 {fileID: tempFileURL}
              const urlMap = {};
              tempRes.result.fileList.forEach(item => {
                if (item.fileID && item.tempFileURL) {
                  urlMap[item.fileID] = item.tempFileURL;
                }
              });

              // 为有cloud://pic的书籍添加picUrl字段
              books.forEach(book => {
                if (book.pic && urlMap[book.pic]) {
                  // 将book.pic更新为可访问的https地址
                  book.pic = urlMap[book.pic];
                }
              });
            }
          }

          // 假设云函数返回结构中包含data数组和total总数
          this.bookList = res.result.data;
          this.total = res.result.total || 0;
        } else {
          this.bookList = [];
          this.total = 0;
        }
      } catch (error) {
        console.error("获取书籍数据失败:", error);
        this.bookList = [];
        this.total = 0;
      } finally {
        this.loading = false;
      }
    },
    /** 表单提交 */
    submitForm() {
      this.$refs["form"].validate(async (valid) => {
        if (valid) {
          if (this.form._id) {
            // 修改书籍
            try {
              // 调用云函数更新书籍
              const c2 = new cloud.Cloud({
                identityless: true,
                resourceAppid: 'wxe26635509bb0f6f3',
                resourceEnv: 'zjh-3g1jn04nc036f36e',
              });

              await c2.init()

              const res = await c2.callFunction({
                name: 'updateBook',
                data: {
                  _id: this.form._id,
                  ...this.form
                }
              });
              if (res.result && res.result.success) {
                this.$modal.msgSuccess("修改成功");
                this.open = false;
                this.getList();
              } else {
                this.$modal.msgError("修改失败");
              }
            } catch (error) {
              console.error("修改书籍失败:", error);
              this.$modal.msgError("修改失败");
            }
          } else {
            // 新增书籍
            try {
              const c3 = new this.cloud.Cloud({
                identityless: true,
                resourceAppid: 'wxe26635509bb0f6f3',
                resourceEnv: 'zjh-3g1jn04nc036f36e',
              });
              await c3.init()
              const res = await c3.callFunction({
                name: 'addBook',
                data: this.form
              });
              if (res.result && res.result.success) {
                this.$modal.msgSuccess("新增成功");
                this.open = false;
                this.getList();
              } else {
                this.$modal.msgError("新增失败");
              }
            } catch (error) {
              console.error("新增书籍失败:", error);
              this.$modal.msgError("新增失败");
            }
          }
          }
      });
    },
    /** 取消按钮 */
    cancel() {
      this.open = false;
      this.resetForm();
    },
    /** 表单重置 */
    resetForm() {
      this.form = {
        _id: null,
        title: "",
        author: "",
        isbn: "",
        publisher: "",
        price: 0,
        pubdate: "",
        pic: "",
        summary: ""
      };
      if (this.$refs.form) {
        this.$refs.form.resetFields();
      }
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.queryParams = {
        pageNum: 1,
        pageSize: 10,
        title: "",
        author: "",
        isbn: ""
      };
      this.getList();
    },
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item._id);
      this.single = selection.length !== 1;
      this.multiple = selection.length === 0;
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.resetForm();
      this.open = true;
      this.title = "添加书籍";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.form = { ...row };
      this.open = true;
      this.title = "修改书籍";
    },
    /** 删除按钮操作 */
    async handleDelete(row) {
      const bookIds = row._id || this.ids;
      this.$modal.confirm(`是否确认删除书籍编号为"${bookIds}"的数据项？`).then(async () => {
        try {
          // 调用云函数删除书籍
          const c4 = new cloud.Cloud({
            identityless: true,
            resourceAppid: 'wxe26635509bb0f6f3',
            resourceEnv: 'zjh-3g1jn04nc036f36e',
          });
          await c4.init()
          const res = await c4.callFunction({
            name: 'deleteBook',
            data: {
              _id: bookIds
            }
          });
          console.log(bookIds)
          console.log(res)
          if (res.result && res.result.success) {
            this.$modal.msgSuccess("删除成功");
            this.getList();
          } else {
            this.$modal.msgError("删除失败");
          }
        } catch (error) {
          console.error("删除书籍失败:", error);
          this.$modal.msgError("删除失败");
        }
      }).catch(() => {
        // 用户取消删除
      });
    },
    /** 导出按钮操作 */
    handleExport() {
      const data = this.bookList.map(item => ({
        '书籍编号': item._id,
        '书名': item.title,
        '作者': item.author,
        'ISBN': item.isbn,
        '出版社': item.publisher,
        '价格': item.price,
        '出版日期': item.pubdate,
        '摘要': item.summary
      }));

      // 将 JSON 数据转换为工作表
      const worksheet = XLSX.utils.json_to_sheet(data);
      // 创建一个新的工作簿
      const workbook = XLSX.utils.book_new();
      // 将工作表添加到工作簿中
      XLSX.utils.book_append_sheet(workbook, worksheet, "Books");

      // 将工作簿转换为二进制数据
      const wbout = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' });

      // 创建 Blob 对象
      const blob = new Blob([wbout], { type: 'application/octet-stream' });
      const url = URL.createObjectURL(blob);

      // 创建一个隐藏的 <a> 标签进行下载
      const link = document.createElement('a');
      link.href = url;
      link.download = `books_${new Date().getTime()}.xlsx`; // 文件名可根据需求修改
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    },
    /** 上传封面图片成功回调 */
    handleUploadSuccess(response, file) {
      // 根据实际的上传接口返回结果调整
      if (response && response.url) {
        this.form.pic = response.url;
      } else {
        this.$modal.msgError("上传图片失败");
      }
    },
    /** 格式化日期 */
    formatDate(date) {
      if (!date) return '';
      const d = new Date(date);
      const year = d.getFullYear();
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    }
  }
};
</script>


