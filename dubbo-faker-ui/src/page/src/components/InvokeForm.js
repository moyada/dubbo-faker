import React from 'react';
import { Tabs, Form, Table, Row, Col, Input, InputNumber, Button, Radio, Modal, message, notification } from 'antd';
import InvokeSelect from '../components/InvokeSelect';
import InvokeCascader from '../components/InvokeCascader';
import request from "../utils/request";

const TabPane = Tabs.TabPane;
const FormItem = Form.Item;
const RadioButton = Radio.Button;
const RadioGroup = Radio.Group;

class InvokeForm extends React.Component {
  state = {
    expand: false,
    type: 'dubbo',
    data: [],
    visible: false,
    index: 1,
  }

  static isJSON(str) {
    if (typeof str === 'string') {
      try {
        JSON.parse(str)
        return true
      } catch (e) {
        console.log(e)
        return false
      }
    }
    return false
  }

  handleSearch = (e) => {
    e.preventDefault()
    this.props.form.validateFields((err, values) => {
      if(err) {
        return
      }
      let payload
      switch (this.state.type) {
        case 'dubbo':
          if(null == values.invokeId || values.invokeId === undefined || values.invokeId.length !== 3) {
            message.error("请选择调用请求")
            return
          }
          if(null == values.invokeExpression || values.invokeExpression === undefined ||
          values.invokeExpression.charAt(0) !== '[' || !InvokeForm.isJSON(values.invokeExpression)) {
            message.error("请输入正确的参数表达式")
            return
          }

          payload = {
            invokeId: values.invokeId[2].split(`-`)[0],
            invokeExpression: values.invokeExpression,
            poolSize: values.poolSize,
            qps: values.qps,
            random: values.random,
            loop: values.loop,
            saveResult: values.saveResult,
            resultParam: values.resultParam,
          }

          message.success("生成测试请求")

          request("faker/invokeDubbo", payload)
            .then(resp => {
              if(resp.err) {
                message.error(resp.err.message, 10)
                return
              }
              if(resp.data.code === 200) {

                notification.open({
                  message: '请求成功',
                  description: resp.data.data,
                  duration: 20
                });
                console.log(resp.data.data)
              }
              else {
                message.error(resp.data.msg, 10)
              }
            })
          break

        case 'http':
          if(0 === this.state.data.length) {
            message.error("请输入请求信息")
            return
          }

          payload = {
            invokeInfo: JSON.stringify(this.state.data),
            poolSize: values.poolSize,
            qps: values.qps,
            loop: values.loop,
            saveResult: values.saveResult,
            resultParam: values.resultParam,
          }

          message.success("生成测试请求")

          this.setState({data: []})

          request("faker/invokeHttp", payload)
            .then(resp => {
              if(resp.err) {
                message.error(resp.err.message, 10)
                return
              }
              if(resp.data.code === 200) {

                notification.open({
                  message: '请求成功',
                  description: resp.data.data,
                  duration: 20
                });
                console.log(resp.data.data)
              }
              else {

              }
            })
          break
      }
    })
  }

  handleReset = () => {
    this.props.form.resetFields()
  }

  onSelectInvoke(value) {
    if(value !== undefined && null !== value && value.length === 3) {
      let values = value[2].split(`-`)
      if(value !== undefined && null !== value) {
        this.props.form.setFieldsValue({
          invokeExpression: values[1],
        })
      }
    }
  }

  showModal = () => {
    this.setState({
      visible: true,
    });
  }

  removeHttp = (key) => {
    return (event) => {
      event.preventDefault();
      let data = this.state.data
      data = data.filter(e => e.key !== key)
      this.setState({
        data: data
      })
    }
  }

  handleOk = (e) => {
    const {url, method, header, cookie, param} = this.props.form.getFieldsValue(["url", "method", "header", "cookie", "param"])
    if(url === undefined || null === url) {

      notification.error({
        message: '错误',
        description: 'url 不能为空',
        duration: 5
      });
      return
    }
    if(method === undefined || null === method) {

      notification.error({
        message: '错误',
        description: '请求方法 不能为空',
        duration: 5
      });
      return
    }

    if(null != header && header !== undefined && header.trim() !== "" &&
      header.charAt(0) === '{' && !InvokeForm.isJSON(header)) {

      notification.error({
        message: '错误',
        description: '请求头非JSON格式',
        duration: 5
      });
      return
    }

    if(null != cookie && cookie !== undefined && cookie.trim() !== "" &&
      cookie.charAt(0) === '{' && !InvokeForm.isJSON(cookie)) {

      notification.error({
        message: '错误',
        description: 'cookie非JSON格式',
        duration: 5
      });
      return
    }

    if(null != param && param !== undefined && param.trim() !== "" &&
      param.charAt(0) === '{' && !InvokeForm.isJSON(param)) {

      notification.error({
        message: '错误',
        description: '请求参数非JSON格式',
        duration: 5
      });
      return
    }


    this.state.data.push({
      key: `_` + this.state.index,
      url: url,
      method: method,
      header: header,
      cookie: cookie,
      param: param,
    })
    this.props.form.resetFields(["url", "method", "header", "cookie", "param"])
    this.setState({
      index: this.state.index + 1,
      visible: false,
    });
  }


  handleCancel = (e) => {
    this.props.form.resetFields(["url", "method", "header", "cookie", "param"])
    this.setState({
      visible: false,
    });
  }

  render() {
    const { getFieldDecorator } = this.props.form;

    const formItemLayout = {
      labelCol: { span: 9 },
      wrapperCol: { span: 15 },
    };

    const formItemRowLayout = {
        labelCol: {span: 2},
        wrapperCol: {span: 22},
      }
    ;

    const columns = [{
      title: '编号',
      dataIndex: 'key',
      width: '5%'
    }, {
      title: '请求链接',
      dataIndex: 'url',
      width: '18%'
    }, {
      title: '请求方法',
      dataIndex: 'method',
      width: '8%'
    }, {
      title: '请求头',
      dataIndex: 'header',
      width: '20%'
    }, {
      title: 'cookie',
      dataIndex: 'cookie',
      width: '20%'
    }, {
      title: '请求参数',
      dataIndex: 'param',
      width: '25%'
    }, {
      title: '操作',
      dataIndex: 'operate',
      width: '5%',
      render: (data, record) => {
        return (
          <div>
            <a onClick={this.removeHttp(record.key)}>删除</a>
          </div>
        )
      }
    }];

    return (
      <Form
        className="ant-advanced-search-form"
        style={{backgroundColor : '#EEEEEE', paddingLeft: 20}}
        onSubmit={this.handleSearch}
      >
        <Row>
          <Tabs defaultActiveKey="dubbo" onChange={(key) => this.state.type = key }>
            <TabPane tab="Dubbo" key="dubbo">
              <Col span={24} key='invokeId'>
                <FormItem {...formItemRowLayout} style={{ marginRight: '100px', marginTop: '20px' }} label={`请求`}>
                  {getFieldDecorator(`invokeId`, {initFieldsValue: null})(
                    <InvokeCascader
                      onChange={this.onSelectInvoke.bind(this)}
                    />
                  )}
                </FormItem>
              </Col>
              <Col span={24} key='invokeExpression'>
                <FormItem {...formItemRowLayout} style={{ marginRight: '100px' }} label={`参数表达式`}>
                  {getFieldDecorator(`invokeExpression`, {initFieldsValue: null})(
                    <Input
                      placeholder='["${1.test}"]'
                    />
                  )}
                </FormItem>
              </Col>
            </TabPane>

            <TabPane tab="Http" key="http">
              <Col span={24} key='invokeUrl'>
                <Table
                  dataSource={this.state.data}
                  columns={columns}
                  pagination={false}
                />
              </Col>

              <Col span={24} key='add' style={{textAlign: 'right', paddingRight: '50px', marginTop: 10 , marginBottom: 10 }} >
                <Button type="primary" onClick={this.showModal}>添加请求信息</Button>
                <Modal
                  title="请求信息"
                  visible={this.state.visible}
                  onOk={this.handleOk.bind(this)}
                  onCancel={this.handleCancel.bind(this)}
                  width="70%"
                >
                  <Row>
                    <Col span={24} key="url">
                      <FormItem {...formItemRowLayout} label={`请求链接`}>
                        {getFieldDecorator(`url`, {initFieldsValue: null})(
                          <Input
                            placeholder='http://'
                          />
                        )}
                      </FormItem>
                    </Col>
                    <Col span={24} key="method">
                      <FormItem {...formItemRowLayout} label={`请求方法`}>
                        {getFieldDecorator(`method`, {initFieldsValue: "get"})(
                          <RadioGroup >
                            <RadioButton value="get">GET</RadioButton>
                            <RadioButton value="post">POST</RadioButton>
                            <RadioButton value="put">PUT</RadioButton>
                            <RadioButton value="delete">DELETE</RadioButton>
                          </RadioGroup>
                        )}
                      </FormItem>
                    </Col>
                    <Col span={24} key="header">
                      <FormItem {...formItemRowLayout} label={`请求头`}>
                        {getFieldDecorator(`header`, {initFieldsValue: null})(
                          <Input
                            placeholder='{"_token": "153957392783"}'
                          />
                        )}
                      </FormItem>
                    </Col>
                    <Col span={24} key="cookie">
                      <FormItem {...formItemRowLayout} label={`cookie`}>
                        {getFieldDecorator(`cookie`, {initFieldsValue: null})(
                          <Input
                            placeholder='{"JSESSIONID": "ByOK3vjFD72aPnrF7C2HmdnV6TZcEbzWoWiBYEnLerjQ99zWpBng"}'
                          />
                        )}
                      </FormItem>
                    </Col>
                    <Col span={24} key="param">
                      <FormItem {...formItemRowLayout} label={`请求参数`}>
                        {getFieldDecorator(`param`, {initFieldsValue: null})(
                          <Input
                            placeholder='{"id": "123"}'
                          />
                        )}
                      </FormItem>
                    </Col>
                  </Row>
                </Modal>
              </Col>

              <Col span={6} key='proxyHost'>
                <FormItem {...formItemLayout} label={`代理主机`}>
                  {getFieldDecorator(`proxyHost`, {initFieldsValue: null})(
                    <Input
                      placeholder='127.0.0.1'
                    />
                  )}
                </FormItem>
              </Col>

              <Col span={6} key='proxyPort'>
                <FormItem {...formItemLayout} label={`代理端口`}>
                  {getFieldDecorator(`proxyPort`, {initFieldsValue: null})(
                    <InputNumber
                      min={0}
                      precision={0}
                      placeholder='80'
                    />
                  )}
                </FormItem>
              </Col>

              <Col span={6} key='proxyUsername'>
                <FormItem {...formItemLayout} label={`账号`}>
                  {getFieldDecorator(`proxyUsername`, {initFieldsValue: null})(
                    <Input
                      placeholder='admin'
                    />
                  )}
                </FormItem>
              </Col>

              <Col span={6} key='proxyPassword'>
                <FormItem {...formItemLayout} style={{ marginRight: '20px' }} label={`密码`}>
                  {getFieldDecorator(`proxyPassword`, {initFieldsValue: null})(
                    <Input
                    />
                  )}
                </FormItem>
              </Col>

            </TabPane>
          </Tabs>

          <Col span={3} key='poolSize'>
            <FormItem {...formItemLayout} label={`并发数`}>
              {getFieldDecorator(`poolSize`, {initFieldsValue: null})(
                <InputNumber
                  min={0}
                  max={1000}
                  precision={0}
                />
              )}
            </FormItem>
          </Col>
          <Col span={4} key='qps'>
            <FormItem {...formItemLayout} label={`每秒请求数`}>
              {getFieldDecorator(`qps`, {initFieldsValue: null})(
                <InputNumber
                  min={0}
                  max={1000}
                  precision={0}
                />
              )}
            </FormItem>
          </Col>
          <Col span={3} key='loop'>
            <FormItem {...formItemLayout} label={`请求次数`}>
              {getFieldDecorator(`loop`, {initFieldsValue: null})(
                <InputNumber
                  min={0}
                  precision={0}
                />
              )}
            </FormItem>
          </Col>
          <Col span={4} key='random'>
            <FormItem {...formItemLayout} label={`随机调用`}>
              {getFieldDecorator(`random`, {initFieldsValue: 1})(
                <RadioGroup >
                  <RadioButton value={1}>是</RadioButton>
                  <RadioButton value={0}>否</RadioButton>
                </RadioGroup>
              )}
            </FormItem>
          </Col>
          <Col span={4} key='saveResult'>
            <FormItem {...formItemLayout} label={`保存结果`}>
              {getFieldDecorator(`saveResult`, {initFieldsValue: false})(
                <RadioGroup >
                  <RadioButton value={true}>是</RadioButton>
                  <RadioButton value={false}>否</RadioButton>
                </RadioGroup>
              )}
            </FormItem>
          </Col>
          <Col span={5} key='resultParam'>
            <FormItem labelCol={{span: 8}} wrapperCol={{span: 16}}
                      label={`保存结果参数`}>
              {getFieldDecorator(`resultParam`, {initFieldsValue: null})(
                <Input
                  disabled={!this.props.form.getFieldValue('saveResult')}
                />
              )}
            </FormItem>
          </Col>
        </Row>
        <Row>
          <Col span={24} style={{ textAlign: 'right', paddingRight: '50px', marginBottom: 10 }}>
            <Button type="primary" htmlType="submit">提交请求</Button>
            <Button style={{ marginLeft: 8 }} onClick={this.handleReset}>
              清除
            </Button>
          </Col>
        </Row>
      </Form>
    );
  }
}

export default Form.create()(InvokeForm);
