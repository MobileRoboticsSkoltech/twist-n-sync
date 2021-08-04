//
// Created by achains on 26.07.2021.
//

#include "CubicSpline.h"
#include "TSUtil.h"

#include <iostream>

CubicSpline::CubicSpline(Eigen::VectorXd const & X, Eigen::VectorXd const & Y):
        m_spline_(std::vector<double>(X.data(), X.data() + X.size()),
                  std::vector<double>(Y.data(), Y.data() + Y.size())){}


double CubicSpline::operator()(double const & x) {
    return m_spline_(x);
}

Eigen::VectorXd CubicSpline::getValuesOnSegment(Eigen::VectorXd const & X) {
    Eigen::VectorXd y_values(X.size());
    for (Eigen::Index i = 0; i < X.size(); ++i)
        y_values[i] = m_spline_(X[i]);

    return y_values;
}

void CubicSpline::calculateDerivative() {
    derivative_.resize(m_spline_.get_x().size());
    size_t i = 0;
    for (auto& elem: m_spline_.get_x()){
        derivative_[i++] = m_spline_.deriv(1, elem);
    }

    derivative_.front() = derivative_.back() = 0.0;
}

Eigen::Matrix4Xd CubicSpline::getCoefficients() {
    if (derivative_.empty()){
        calculateDerivative();
    }
    std::vector<double> Y = m_spline_.get_y();
    Eigen::VectorXd A = TSUtil::vectorToEigVectorXd(Y);
    Eigen::VectorXd B = TSUtil::vectorToEigVectorXd(derivative_);

    double dx = m_spline_.get_x()[1] - m_spline_.get_x()[0];

    Eigen::VectorXd C(A.size() - 1);
    Eigen::VectorXd D(A.size() - 1);

    for (Eigen::Index i = 0; i < A.size() - 1; ++i){
        C[i] = -(2.0 * B[i] + B[i + 1]) / dx + 3.0 * (A[i + 1] - A[i]) / (dx * dx);
        D[i] = -2.0 * C[i] / (3. * dx) + (B[i + 1] - B[i]) / (3 * dx * dx);
    }

    Eigen::Matrix4Xd coefficients(4, A.size() - 1);

    // There are N - 1 segments, where N is number of knots.
    // C, D already have A.size() - 1, where A.size() equals N
    // Getting rid of the last redundant element in A and B.
    coefficients << D.transpose(), C.transpose(),
                    B.block(0, 0, A.size() - 1, 1).transpose(),
                    A.block(0, 0, A.size() - 1, 1).transpose();


    return coefficients;
}